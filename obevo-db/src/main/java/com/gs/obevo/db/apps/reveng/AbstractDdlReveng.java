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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;

public abstract class AbstractDdlReveng {
    private final DbPlatform platform;
    private final MultiLineStringSplitter stringSplitter;
    private final ImmutableList<Predicate<String>> skipPredicates;
    private ImmutableList<Predicate<String>> skipLinePredicates;
    private final ImmutableList<RevengPattern> revengPatterns;
    private final Procedure2<ChangeEntry, String> postProcessChange;

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
        return "(?:" + namePattern(startQuoteStr, endQuoteStr) + "\\.)?" + "(?:" + namePattern(startQuoteStr, endQuoteStr) + "\\.)?" + namePattern(startQuoteStr, endQuoteStr);
    }
    protected static String getSchemaObjectPattern(String startQuoteStr, String endQuoteStr) {
        return "(?:" + namePattern(startQuoteStr, endQuoteStr) + "\\.)?" + namePattern(startQuoteStr, endQuoteStr);
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
        if (!isNativeRevengSupported() && args.getInputPath() == null) {
            printInstructions(args);
        } else {
            revengMain(args);
        }
    }

    public void setSkipLinePredicates(ImmutableList<Predicate<String>> skipLinePredicates) {
        this.skipLinePredicates = skipLinePredicates;
    }

    protected abstract void printInstructions(AquaRevengArgs args);

    protected boolean isNativeRevengSupported() {
        return false;
    }

    protected File doNativeReveng(AquaRevengArgs args, DbEnvironment env) {
        return null;
    }

    private void revengMain(AquaRevengArgs args) {
        String schema = args.getDbSchema();
        File file;
        if (args.getInputPath() != null) {
            file = args.getInputPath();
        } else if (isNativeRevengSupported()) {
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

            file = doNativeReveng(args, env);
        } else {
            throw new IllegalStateException("Can't reach here");
        }
        boolean generateBaseline = args.isGenerateBaseline();
        File outputDir = args.getOutputPath();

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

        String data = dataLines
                .reject(skipLinePredicates != null ? Predicates.or(skipLinePredicates) : (Predicate) Predicates.alwaysFalse())
                .makeString(SystemUtils.LINE_SEPARATOR);

        MutableList<String> entries = stringSplitter.valueOf(data);

        int selfOrder = 0;


        // Find object names
        MutableSet<String> objectNames = Sets.mutable.empty();
        for (String candidateLine : entries) {
            candidateLine = StringUtils.stripStart(candidateLine, "\r\n \t");

            if (StringUtils.isNotBlank(candidateLine)
                    && Predicates.noneOf(skipPredicates).accept(candidateLine)
                    ) {
/*
                candidateLine = candidateLine.replaceAll(schema + "\\.dbo\\.", "");  // sybase ASE
                candidateLine = candidateLine.replaceAll("'dbo\\.", "'");  // sybase ASE
                candidateLine = candidateLine.replaceAll("\"" + schema + "\\s*\"\\.", "");  // DB2
                candidateLine = candidateLine.replaceAll(schema + "\\.", "");  // alternate DB2 for views
                candidateLine = removeQuotesFromProcxmode(candidateLine);  // sybase ASE
*/

                for (RevengPattern revengPattern : revengPatterns) {
                    RevengPatternOutput patternMatch = revengPattern.evaluate(candidateLine);
                    if (patternMatch != null) {
                        System.out.println("OBJECT NAME " + patternMatch.getPrimaryName() + ":" + patternMatch.getSecondaryName());
                        objectNames.add(patternMatch.getPrimaryName());
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
/*
                    for (String objectName : objectNames) {
                        candidateLine = candidateLine.replaceAll(schema + "\\s*\\." + objectName, objectName);  // sybase ASE
                        candidateLine = candidateLine.replaceAll(schema.toLowerCase() + "\\s*\\." + objectName.toLowerCase(), objectName.toLowerCase());  // sybase ASE
                    }
*/
/*
                    candidateLine = candidateLine.replaceAll(schema + "\\.dbo\\.", "");  // sybase ASE
                    candidateLine = candidateLine.replaceAll("'dbo\\.", "'");  // sybase ASE
*/

/*
                    candidateLine = candidateLine.replaceAll("\"" + schema + "\\s*\"\\.", "");  // DB2
                    candidateLine = candidateLine.replaceAll(schema + "\\.", "");  // alternate DB2 for views
*/
                    candidateLine = removeQuotesFromProcxmode(candidateLine);  // sybase ASE

                    RevengPattern chosenRevengPattern = null;
                    String secondaryName = null;
                    for (RevengPattern revengPattern : revengPatterns) {
                        RevengPatternOutput patternMatch = revengPattern.evaluate(candidateLine);
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

        new RevengWriter().write(platform, changeEntries, outputDir, generateBaseline, RevengWriter.defaultShouldOverwritePredicate(), args.getJdbcUrl(), args.getDbHost(), args.getDbPort(), args.getDbServer());
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

    public static class RevengPattern {
        private final String changeType;
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

        public RevengPattern(String changeType, String pattern) {
            this(changeType, pattern, 1);
        }

        public RevengPattern(String changeType, String pattern, int primaryNameIndex) {
            this(changeType, pattern, primaryNameIndex, null, null);
        }

        public RevengPattern(String changeType, String pattern, int primaryNameIndex, Integer secondaryNameIndex, String annotation) {
            this.changeType = changeType;
            this.pattern = Pattern.compile(pattern, Pattern.DOTALL);
            this.primaryNameIndex = primaryNameIndex;
            this.secondaryNameIndex = secondaryNameIndex;
            this.annotation = annotation;
        }

        public String getChangeType() {
            return changeType;
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

        public RevengPatternOutput evaluate(String input) {
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                String primaryName = matcher.group(primaryNameIndex);
                String secondaryName = null;
                if (secondaryNameIndex != null) {
                    secondaryName = matcher.group(secondaryNameIndex);
                }
                return new RevengPatternOutput(primaryName, secondaryName, input);
            }

            return null;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("changeType", changeType)
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
        private final String revisedLine;

        public RevengPatternOutput(String primaryName, String secondaryName, String revisedLine) {
            this.primaryName = primaryName;
            this.secondaryName = secondaryName;
            this.revisedLine = revisedLine;
        }

        public String getPrimaryName() {
            return primaryName;
        }

        public String getSecondaryName() {
            return secondaryName;
        }

        public String getRevisedLine() {
            return revisedLine;
        }
    }
}
