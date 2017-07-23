/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevo.db.apps.reveng;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.impl.core.util.MultiLineStringSplitter;
import com.gs.obevo.impl.changetypes.UnclassifiedChangeType;
import com.gs.obevo.util.FileUtilsCobra;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDdlReveng {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDdlReveng.class);

    private final DbPlatform platform;
    private final MultiLineStringSplitter stringSplitter;
    private final ImmutableList<Predicate<String>> skipPredicates;
    private ImmutableList<Predicate<String>> skipLinePredicates;
    private final ImmutableList<RevengPattern> revengPatterns;
    private final Procedure2<ChangeEntry, String> postProcessChange;
    private String startQuote = "";
    private String endQuote = "";
    private boolean skipSchemaValidation = false;

    public static String removeQuotes(String input) {
        Pattern compile = Pattern.compile("\"([A-Z_0-9]+)\"", Pattern.DOTALL);

        StringBuffer sb = new StringBuffer(input.length());

        Matcher matcher = compile.matcher(input);
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static AbstractDdlReveng.LineParseOutput substituteTablespace(String input) {
        Pattern compile = Pattern.compile("(\\s+IN\\s+)\"(\\w+)\"(\\s*)", Pattern.DOTALL);

        StringBuffer sb = new StringBuffer(input.length());

        String addedToken = null;
        String addedValue = null;
        Matcher matcher = compile.matcher(input);
        if (matcher.find()) {
            addedToken = matcher.group(2) + "_token";
            addedValue = matcher.group(2);
            matcher.appendReplacement(sb, matcher.group(1) + "\"\\${" + addedToken + "}\"" + matcher.group(3));
        }
        matcher.appendTail(sb);

        return new AbstractDdlReveng.LineParseOutput(sb.toString()).withToken(addedToken, addedValue);
    }

    protected static final Function<String, LineParseOutput> REMOVE_QUOTES = new Function<String, AbstractDdlReveng.LineParseOutput>() {
        @Override
        public AbstractDdlReveng.LineParseOutput valueOf(String input) {
            return new AbstractDdlReveng.LineParseOutput(removeQuotes(input));
        }
    };
    protected static final Function<String, AbstractDdlReveng.LineParseOutput> REPLACE_TABLESPACE = new Function<String, AbstractDdlReveng.LineParseOutput>() {
        @Override
        public AbstractDdlReveng.LineParseOutput valueOf(String input) {
            return substituteTablespace(input);
        }
    };

    public AbstractDdlReveng(DbPlatform platform, MultiLineStringSplitter stringSplitter, ImmutableList<Predicate<String>> skipPredicates, ImmutableList<RevengPattern> revengPatterns, Procedure2<ChangeEntry, String> postProcessChange) {
        this.platform = platform;
        this.stringSplitter = stringSplitter;
        this.skipPredicates = skipPredicates;
        this.revengPatterns = revengPatterns;
        this.postProcessChange = postProcessChange;
    }

    protected static String getCatalogSchemaObjectPattern(String startQuoteStr, String endQuoteStr) {
        return    "(?:" + namePattern(startQuoteStr, endQuoteStr) + "\\.)?"
                + "(?:" + namePattern(startQuoteStr, endQuoteStr) + "\\.)?" + namePattern(startQuoteStr, endQuoteStr);
    }
    protected static String getSchemaObjectPattern(String startQuoteStr, String endQuoteStr) {
        return "(?:" + namePattern(startQuoteStr, endQuoteStr) + "\\.)?" + namePattern(startQuoteStr, endQuoteStr);
    }
    protected static String getObjectPattern(String startQuoteStr, String endQuoteStr) {
        return namePattern(startQuoteStr, endQuoteStr);
    }

    protected static String getSchemaObjectWithPrefixPattern(String startQuoteStr, String endQuoteStr, String objectNamePrefix) {
        return "(?:" + namePattern(startQuoteStr, endQuoteStr) + "\\.)?" + nameWithPrefixPattern(startQuoteStr, endQuoteStr, objectNamePrefix);
    }

    private static String namePattern(String startQuoteStr, String endQuoteStr) {
        return "(?:(?:" + startQuoteStr + ")?(\\w+)(?:" + endQuoteStr + ")?)";
    }
    private static String nameWithPrefixPattern(String startQuoteStr, String endQuoteStr, String prefix) {
        return "(?:(?:" + startQuoteStr + ")?(" + prefix + "\\w+)(?:" + endQuoteStr + ")?)";
    }

    public void reveng(AquaRevengArgs args) {
        if (args.getInputPath() == null) {
            File file = printInstructions(System.out, args);
            System.out.println("");
            System.out.println("");
            if (file != null) {
                System.out.println("Interim reverse-engineering from the vendor tool is complete.");
                System.out.println("Content was written to: " + file);
                System.out.println("Proceeding with full reverse-engineering: " + file);
                System.out.println("");
                System.out.println("*** In case the interim content had issues when reverse-engineering to the final output, you can update the interim files and restart from there (without going back to the DB) by specifying the following argument:");
                System.out.println("    -inputPath " + ObjectUtils.defaultIfNull(args.getOutputPath(), "<outputFile>"));
                revengMain(file, args);
            } else {
                System.out.println("***********");
                System.out.println("");
                System.out.println("Once those steps are done, rerun the reverse-engineering command you just ran, but add the following argument based on the <outputDirectory> value passed in above the argument:");
                System.out.println("    -inputPath " + ObjectUtils.defaultIfNull(args.getOutputPath(), "<outputFile>"));
                System.out.println("");
                System.out.println("If you need more information on the vendor reverse engineer process, see the doc: https://goldmansachs.github.io/obevo/reverse-engineer-dbmstools.html");
            }
        } else {
            revengMain(args.getInputPath(), args);
        }
    }

    public void setSkipLinePredicates(ImmutableList<Predicate<String>> skipLinePredicates) {
        this.skipLinePredicates = skipLinePredicates;
    }

    public void setStartQuote(String startQuote) {
        this.startQuote = startQuote;
    }

    public void setEndQuote(String endQuote) {
        this.endQuote = endQuote;
    }

    /**
     * Temporary feature to allow us to handle subschemas in MS SQL. We should retire this once we fully support
     * database + schema combos in Obevo.
     */
    public void setSkipSchemaValidation(boolean skipSchemaValidation) {
        this.skipSchemaValidation = skipSchemaValidation;
    }

    /**
     * Either generate the file or directory with the DB schema information to reverse engineer given the input args,
     * or print out instructions for the user on how to generate it.
     *
     * @param out The printstream to use in the implementing function to give output to the user.
     * @param args The db args to reverse engineer
     * @return The file or directory that has the reverse-engineered content, or null if the user should instead invoke the reverse-engineering command separately
     */
    protected abstract File printInstructions(PrintStream out, AquaRevengArgs args);

    private void revengMain(File file, AquaRevengArgs args) {
        String schema = args.getDbSchema();

        boolean generateBaseline = args.isGenerateBaseline();

        MutableList<ChangeEntry> changeEntries = Lists.mutable.empty();

        final MutableList<String> dataLines;
        if (file.isFile()) {
            dataLines = FileUtilsCobra.readLines(file);
        } else {
            final MutableList<File> files = Lists.mutable.empty();
            try {
                Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        files.add(file.toFile());
                        return super.visitFile(file, attrs);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            dataLines = files
                    .select(new Predicate<File>() {
                        @Override
                        public boolean accept(File file) {
                            return file.isFile();
                        }
                    }).flatCollect(new Function<File, Iterable<String>>() {
                        @Override
                        public Iterable<String> valueOf(File file) {
                            return FileUtilsCobra.readLines(file);
                        }
                    });
        }

        dataLines.forEachWithIndex(new ObjectIntProcedure<String>() {
            @Override
            public void value(String line, int i) {
                if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
                    dataLines.set(i, dataLines.get(i).substring(1));
                }
                if (line.startsWith("--------------------")
                        && dataLines.get(i + 1).startsWith("-- DDL Statements")
                        && dataLines.get(i + 2).startsWith("--------------------")) {
                    dataLines.set(i, "");
                    dataLines.set(i + 1, "");
                    dataLines.set(i + 2, "");
                } else if (line.startsWith("--------------------")
                        && dataLines.get(i + 2).startsWith("-- DDL Statements")
                        && dataLines.get(i + 4).startsWith("--------------------")) {
                    dataLines.set(i, "");
                    dataLines.set(i + 1, "");
                    dataLines.set(i + 2, "");
                    dataLines.set(i + 3, "");
                    dataLines.set(i + 4, "");
                } else if (line.startsWith("-- DDL Statements for ")) {
                    dataLines.set(i, "");
                }

                // For PostgreSQL
                if ((line.equals("--")
                        && dataLines.get(i + 1).startsWith("-- Name: ")
                        && dataLines.get(i + 2).equals("--"))) {
                    dataLines.set(i, "");
                    dataLines.set(i + 1, "GO");
                    dataLines.set(i + 2, "");
                }
            }
        });

        MutableList<String> entries;
        if (stringSplitter != null) {
            String data = dataLines
                    .reject(skipLinePredicates != null ? Predicates.or(skipLinePredicates) : (Predicate) Predicates.alwaysFalse())
                    .makeString(SystemUtils.LINE_SEPARATOR);

            entries = stringSplitter.valueOf(data);
        } else {
            // If null, then default each line to being its own parsable statement
            entries = dataLines
                    .reject(skipLinePredicates != null ? Predicates.or(skipLinePredicates) : (Predicate) Predicates.alwaysFalse());
        }

        int selfOrder = 0;


        // Find object names
        MutableSet<RevengPatternOutput> objectNames = Sets.mutable.empty();
        MutableSetMultimap<String, String> objectToSchemasMap = Multimaps.mutable.set.empty();
        MutableSetMultimap<String, String> objectToSubSchemasMap = Multimaps.mutable.set.empty();
        for (String candidateLine : entries) {
            candidateLine = StringUtils.stripStart(candidateLine, "\r\n \t");

            if (StringUtils.isNotBlank(candidateLine) && Predicates.noneOf(skipPredicates).accept(candidateLine)) {
                for (RevengPattern revengPattern : revengPatterns) {
                    RevengPatternOutput patternMatch = revengPattern.evaluate(candidateLine);
                    if (patternMatch != null) {
                        LOG.debug("Found object: {}", patternMatch);
                        objectNames.add(patternMatch);
                        if (patternMatch.getSchema() != null) {
                            objectToSchemasMap.put(patternMatch.getPrimaryName(), patternMatch.getSchema());
                        }
                        if (patternMatch.getSubSchema() != null) {
                            objectToSubSchemasMap.put(patternMatch.getPrimaryName(), patternMatch.getSubSchema());
                        }
                        break;
                    }
                }
            }
        }

        MutableMap<String, AtomicInteger> countByObject = Maps.mutable.empty();

        String candidateObject = "UNKNOWN";
        ChangeType candidateObjectType = UnclassifiedChangeType.INSTANCE;
        for (String candidateLine : entries) {
            try {

                candidateLine = StringUtils.stripStart(candidateLine, "\r\n \t");

                if (StringUtils.isNotBlank(candidateLine)
                        && Predicates.noneOf(skipPredicates).accept(candidateLine)
                        ) {
                    candidateLine = removeQuotesFromProcxmode(candidateLine);  // sybase ASE

                    RevengPattern chosenRevengPattern = null;
                    String secondaryName = null;
                    RevengPatternOutput patternMatch = null;
                    for (RevengPattern revengPattern : revengPatterns) {
                        patternMatch = revengPattern.evaluate(candidateLine);
                        if (patternMatch != null) {
                            chosenRevengPattern = revengPattern;
                            candidateObject = patternMatch.getPrimaryName();
                            if (patternMatch.getSecondaryName() != null) {
                                secondaryName = patternMatch.getSecondaryName();
                            }
                            candidateObjectType = platform.getChangeType(revengPattern.getChangeType());
                            break;
                        }
                    }

                    // Ignore other schemas that may have been found in your parsing (came up during HSQLDB use case)
                    if (!skipSchemaValidation && patternMatch != null && patternMatch.getSchema() != null && patternMatch.getSubSchema() == null && !patternMatch.getSchema().equalsIgnoreCase(schema)) {
                        continue;
                    }

                    for (RevengPatternOutput objectOutput : objectNames) {
                        MutableSet<String> replacerSchemas = objectToSchemasMap.get(objectOutput.getPrimaryName());
                        if (replacerSchemas == null || replacerSchemas.isEmpty()) {
                            replacerSchemas = objectToSchemasMap.valuesView().toSet();
                        }
                        MutableSet<String> replacerSubSchemas = objectToSubSchemasMap.get(objectOutput.getPrimaryName());
                        if (replacerSubSchemas == null || replacerSubSchemas.isEmpty()) {
                            replacerSubSchemas = objectToSubSchemasMap.valuesView().toSet();
                        }
                        LOG.debug("Using replacer schemas {} and subschemas {} on object {}", replacerSchemas, replacerSubSchemas, objectOutput.getPrimaryName());

                        if (replacerSubSchemas.notEmpty()) {
                            LazyIterable<Pair<String, String>> pairs = replacerSchemas.cartesianProduct(replacerSubSchemas);
                            for (Pair<String, String> pair : pairs) {
                                String replacerSchema = pair.getOne();
                                String replacerSubSchema = pair.getTwo();
                                for (boolean useQuotes : Lists.fixedSize.of(true, false)) {
                                    String sQuote = useQuotes ? startQuote : "";
                                    String eQuote = useQuotes ? endQuote : "";
                                    candidateLine = candidateLine.replaceAll(sQuote + replacerSchema + "\\s*" + eQuote + "\\." + sQuote + replacerSubSchema + "\\s*" + eQuote + "\\." + sQuote + objectOutput.getPrimaryName() + eQuote, objectOutput.getPrimaryName());
                                    candidateLine = candidateLine.replaceAll(sQuote + replacerSchema + "\\s*" + eQuote + "\\." + sQuote + "\\s*" + eQuote + "\\." + sQuote + objectOutput.getPrimaryName() + eQuote, objectOutput.getPrimaryName());
                                    candidateLine = candidateLine.replaceAll(sQuote + replacerSubSchema + "\\s*" + eQuote + "\\." + sQuote + objectOutput.getPrimaryName() + eQuote, objectOutput.getPrimaryName());
                                    if (objectOutput.getSecondaryName() != null) {
                                        candidateLine = candidateLine.replaceAll(sQuote + replacerSchema + "\\s*" + eQuote + "\\." + sQuote + replacerSubSchema + "\\s*" + eQuote + "\\." + sQuote + objectOutput.getSecondaryName() + eQuote, objectOutput.getSecondaryName());
                                        candidateLine = candidateLine.replaceAll(sQuote + replacerSchema + "\\s*" + eQuote + "\\." + sQuote + "\\s*" + eQuote + "\\." + sQuote + objectOutput.getSecondaryName() + eQuote, objectOutput.getSecondaryName());
                                        candidateLine = candidateLine.replaceAll(sQuote + replacerSubSchema + "\\s*" + eQuote + "\\." + sQuote + objectOutput.getSecondaryName() + eQuote, objectOutput.getSecondaryName());
                                    }
                                }
                                LOG.info("MY PAIRS! {}", pair);
                            }
                        } else {
                            for (String replacerSchema : replacerSchemas) {
                                candidateLine = candidateLine.replaceAll(startQuote + replacerSchema + "\\s*" + endQuote + "\\." + startQuote + objectOutput.getPrimaryName() + endQuote, objectOutput.getPrimaryName());
                                candidateLine = candidateLine.replaceAll(replacerSchema + "\\s*" + "\\." + objectOutput.getPrimaryName(), objectOutput.getPrimaryName());
                                if (objectOutput.getSecondaryName() != null) {
                                    candidateLine = candidateLine.replaceAll(startQuote + replacerSchema + "\\s*" + endQuote + "\\." + startQuote + objectOutput.getSecondaryName() + endQuote, objectOutput.getSecondaryName());
                                    candidateLine = candidateLine.replaceAll(replacerSchema + "\\s*" + "\\." + objectOutput.getSecondaryName(), objectOutput.getSecondaryName());
                                }
                            }
                        }
                    }

                    AtomicInteger objectOrder2 = countByObject.getIfAbsentPut(candidateObject, new Function0<AtomicInteger>() {
                        @Override
                        public AtomicInteger value() {
                            return new AtomicInteger(0);
                        }
                    });

                    if (secondaryName == null) {
                        secondaryName = "change" + objectOrder2.getAndIncrement();
                    }
                    RevEngDestination destination = new RevEngDestination(schema, candidateObjectType, candidateObject, false);

                    String annotation = chosenRevengPattern != null ? chosenRevengPattern.getAnnotation() : null;
                    MutableList<Function<String, LineParseOutput>> postProcessSqls = chosenRevengPattern != null ? chosenRevengPattern.getPostProcessSqls() : Lists.mutable.<Function<String, LineParseOutput>>empty();

                    for (Function<String, LineParseOutput> postProcessSql : postProcessSqls) {
                        LineParseOutput lineParseOutput = postProcessSql.valueOf(candidateLine);
                        candidateLine = lineParseOutput.getLineOutput();
                    }

                    ChangeEntry change = new ChangeEntry(destination, candidateLine + "\nGO", secondaryName, annotation, selfOrder++);

                    postProcessChange.value(change, candidateLine);

                    changeEntries.add(change);
                }
            } catch (RuntimeException e) {
                throw new RuntimeException("Failed parsing on statement " + candidateLine, e);
            }
        }

        new RevengWriter().write(platform, changeEntries, new File(args.getOutputPath(), "final"), generateBaseline, RevengWriter.defaultShouldOverwritePredicate(), args.getJdbcUrl(), args.getDbHost(), args.getDbPort(), args.getDbServer(), args.getExcludeObjects());
    }

    protected DbEnvironment getDbEnvironment(AquaRevengArgs args) {
        DbEnvironment env = new DbEnvironment();
        env.setPlatform(platform);
        env.setSystemDbPlatform(platform);
        env.setDbHost(args.getDbHost());
        if (args.getDbPort() != null) {
            env.setDbPort(args.getDbPort());
        }
        env.setDbServer(args.getDbServer());
        env.setJdbcUrl(args.getJdbcUrl());
        if (args.getDriverClass() != null) {
            env.setDriverClassName(args.getDriverClass());
        } else {
            env.setDriverClassName(platform.getDriverClass(env).getName());
        }
        return env;
    }

    /**
     * TODO move to Sybase subclass.
     */
    public String removeQuotesFromProcxmode(String input) {
        Pattern compile = Pattern.compile("sp_procxmode '(?:\")(.*?)(?:\")'", Pattern.DOTALL);

        Matcher matcher = compile.matcher(input);
        if (matcher.find()) {
            return matcher.replaceAll("sp_procxmode '" + matcher.group(1) + "'");
        } else {
            return input;
        }
    }

    public static class LineParseOutput {
        private String lineOutput;
        private MutableMap<String, String> tokens = Maps.mutable.empty();

        public LineParseOutput() {
        }

        public LineParseOutput(String lineOutput) {
            this.lineOutput = lineOutput;
        }

        public String getLineOutput() {
            return lineOutput;
        }

        public void setLineOutput(String lineOutput) {
            this.lineOutput = lineOutput;
        }

        public MutableMap<String, String> getTokens() {
            return tokens;
        }

        public void addToken(String key, String value) {
            tokens.put(key, value);
        }

        public LineParseOutput withToken(String key, String value) {
            tokens.put(key, value);
            return this;
        }
    }

    public enum NamePatternType {
        ONE (1),
        TWO (2),
        THREE (3),
        ;

        private final int numParts;

        NamePatternType(int numParts) {
            this.numParts = numParts;
        }

        public Integer getSchemaIndex(int groupIndex) {
            switch (numParts) {
            case 2:
                return groupIndex * numParts - 1;
            case 3:
                return groupIndex * numParts - 2;
            default:
                return null;
            }
        }

        public Integer getSubSchemaIndex(int groupIndex) {
            switch (numParts) {
            case 3:
                return groupIndex * numParts - 1;
            default:
                return null;
            }
        }

        public int getObjectIndex(int groupIndex) {
            return groupIndex * numParts;
        }
    }

    public static class RevengPattern {
        private final String changeType;
        private final NamePatternType namePatternType;
        private final Pattern pattern;
        private final int primaryNameIndex;
        private final Integer secondaryNameIndex;
        private final String annotation;
        private final MutableList<Function<String, LineParseOutput>> postProcessSqls = Lists.mutable.empty();

        public static final Function<RevengPattern, String> TO_CHANGE_TYPE = new Function<RevengPattern, String>() {
            @Override
            public String valueOf(RevengPattern revengPattern) {
                return revengPattern.getChangeType();
            }
        };

        public RevengPattern(String changeType, NamePatternType namePatternType, String pattern) {
            this(changeType, namePatternType, pattern, 1);
        }

        private RevengPattern(String changeType, NamePatternType namePatternType, String pattern, int primaryNameIndex) {
            this(changeType, namePatternType, pattern, primaryNameIndex, null, null);
        }

        public RevengPattern(String changeType, NamePatternType namePatternType, String pattern, int primaryNameIndex, Integer secondaryNameIndex, String annotation) {
            this.changeType = changeType;
            this.namePatternType = namePatternType;
            this.pattern = Pattern.compile(pattern, Pattern.DOTALL);
            this.primaryNameIndex = primaryNameIndex;
            this.secondaryNameIndex = secondaryNameIndex;
            this.annotation = annotation;
        }

        public String getChangeType() {
            return changeType;
        }

        public NamePatternType getNamePatternType() {
            return namePatternType;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public int getPrimaryNameIndex() {
            return primaryNameIndex;
        }

        public Integer getSecondaryNameIndex() {
            return secondaryNameIndex;
        }

        public String getAnnotation() {
            return annotation;
        }

        public MutableList<Function<String, LineParseOutput>> getPostProcessSqls() {
            return postProcessSqls;
        }

        public RevengPattern withPostProcessSql(Function<String, LineParseOutput> postProcessSql) {
            this.postProcessSqls.add(postProcessSql);
            return this;
        }

        private String getme(Matcher matcher, Integer index) {
            if (index == null) {
                return null;
            }
            return matcher.group(index);
        }

        public RevengPatternOutput evaluate(String input) {
            final Matcher matcher = pattern.matcher(input);

            if (matcher.find()) {
                String primaryName = matcher.group(namePatternType.getObjectIndex(primaryNameIndex));
                String schema = getme(matcher, namePatternType.getSchemaIndex(primaryNameIndex));
                String subSchema = getme(matcher, namePatternType.getSubSchemaIndex(primaryNameIndex));

                String secondaryName = null;
                if (secondaryNameIndex != null) {
                    secondaryName = matcher.group(namePatternType.getObjectIndex(secondaryNameIndex));
                    if (schema == null) {
                        schema = getme(matcher, namePatternType.getSchemaIndex(secondaryNameIndex));
                    }
                    if (subSchema == null) {
                        subSchema = getme(matcher, namePatternType.getSubSchemaIndex(secondaryNameIndex));
                    }

                }
                return new RevengPatternOutput(primaryName, secondaryName, schema, subSchema, input);
            }

            return null;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("changeType", changeType)
                    .append("namePatternType", namePatternType)
                    .append("pattern", pattern)
                    .append("primaryNameIndex", primaryNameIndex)
                    .append("secondaryNameIndex", secondaryNameIndex)
                    .append("annotation", annotation)
                    .append("postProcessSqls", postProcessSqls)
                    .toString();
        }
    }

    public static class RevengPatternOutput {
        private final String primaryName;
        private final String secondaryName;
        private final String schema;
        private final String subSchema;
        private final String revisedLine;

        public RevengPatternOutput(String primaryName, String secondaryName, String schema, String subSchema, String revisedLine) {
            this.primaryName = primaryName;
            this.secondaryName = secondaryName;
            this.schema = schema;
            this.subSchema = subSchema;
            this.revisedLine = revisedLine;
        }

        public String getPrimaryName() {
            return primaryName;
        }

        public String getSecondaryName() {
            return secondaryName;
        }

        public String getSchema() {
            return schema;
        }

        public String getSubSchema() {
            return subSchema;
        }

        public String getRevisedLine() {
            return revisedLine;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("schema", schema)
                    .append("subSchema", subSchema)
                    .append("primaryName", primaryName)
                    .append("secondaryName", secondaryName)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RevengPatternOutput that = (RevengPatternOutput) o;
            return Objects.equals(primaryName, that.primaryName) &&
                    Objects.equals(secondaryName, that.secondaryName) &&
                    Objects.equals(schema, that.schema) &&
                    Objects.equals(subSchema, that.subSchema);
        }

        @Override
        public int hashCode() {
            return Objects.hash(primaryName, secondaryName, schema, subSchema);
        }
    }
}
