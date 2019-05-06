/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.impl.changetypes.UnclassifiedChangeType;
import com.gs.obevo.impl.util.MultiLineStringSplitter;
import com.gs.obevo.util.FileUtilsCobra;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
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
import org.eclipse.collections.api.partition.list.PartitionList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDdlReveng {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDdlReveng.class);
    private static final String WORD_REGEX = "[\\w\\#]";

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

    public static LineParseOutput substituteTablespace(String input) {
        Pattern compile = Pattern.compile("(\\s+IN\\s+)\"(" + WORD_REGEX + "+)\"(\\s*)", Pattern.DOTALL);

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

        return new LineParseOutput(sb.toString()).withToken(addedToken, addedValue);
    }

    protected static final Function<String, LineParseOutput> REMOVE_QUOTES = new Function<String, LineParseOutput>() {
        @Override
        public LineParseOutput valueOf(String input) {
            return new LineParseOutput(removeQuotes(input));
        }
    };
    protected static final Function<String, LineParseOutput> REPLACE_TABLESPACE = new Function<String, LineParseOutput>() {
        @Override
        public LineParseOutput valueOf(String input) {
            return substituteTablespace(input);
        }
    };

    protected AbstractDdlReveng(DbPlatform platform, MultiLineStringSplitter stringSplitter, ImmutableList<Predicate<String>> skipPredicates, ImmutableList<RevengPattern> revengPatterns, Procedure2<ChangeEntry, String> postProcessChange) {
        this.platform = platform;
        this.stringSplitter = stringSplitter;
        this.skipPredicates = skipPredicates;
        this.revengPatterns = revengPatterns;
        Procedure2<ChangeEntry, String> noOpProcedure = new Procedure2<ChangeEntry, String>() {
            @Override
            public void value(ChangeEntry changeEntry, String s) {
            }
        };
        this.postProcessChange = ObjectUtils.firstNonNull(postProcessChange, noOpProcedure);
    }

    protected static String getCatalogSchemaObjectPattern(String startQuoteStr, String endQuoteStr) {
        return "(?:" + namePattern(startQuoteStr, endQuoteStr) + "\\.)?"
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
        return "(?:(?:" + startQuoteStr + ")?(" + WORD_REGEX + "+)(?:" + endQuoteStr + ")?)";
    }

    private static String nameWithPrefixPattern(String startQuoteStr, String endQuoteStr, String prefix) {
        return "(?:(?:" + startQuoteStr + ")?(" + prefix + WORD_REGEX + "+)(?:" + endQuoteStr + ")?)";
    }

    public void reveng(AquaRevengArgs args) {
        if (args.getInputPath() == null) {
            File interimDir = new File(args.getOutputPath(), "interim");
            System.out.println();
            boolean proceedWithReveng = doRevengOrInstructions(System.out, args, interimDir);
            System.out.println();
            System.out.println();
            if (proceedWithReveng) {
                System.out.println("Interim reverse-engineering from the vendor tool is complete.");
                System.out.println("Content was written to: " + interimDir);
                System.out.println("Proceeding with full reverse-engineering: " + interimDir);
                System.out.println();
                System.out.println("*** In case the interim content had issues when reverse-engineering to the final output, you can update the interim files and restart from there (without going back to the DB) by specifying the following argument:");
                System.out.println("    -inputPath " + ObjectUtils.defaultIfNull(args.getOutputPath(), "<outputFile>"));
                revengMain(interimDir, args);
            } else {
                System.out.println("***********");
                System.out.println();
                System.out.println("Once those steps are done, rerun the reverse-engineering command you just ran, but add the following argument based on the <outputDirectory> value passed in above the argument:");
                System.out.println("    -inputPath " + interimDir.getAbsolutePath());
                System.out.println();
                System.out.println("If you need more information on the vendor reverse engineer process, see the doc: https://goldmansachs.github.io/obevo/reverse-engineer-dbmstools.html");
            }
        } else {
            revengMain(args.getInputPath(), args);
        }
    }

    protected void setSkipLinePredicates(ImmutableList<Predicate<String>> skipLinePredicates) {
        this.skipLinePredicates = skipLinePredicates;
    }

    protected void setStartQuote(String startQuote) {
        this.startQuote = startQuote;
    }

    protected void setEndQuote(String endQuote) {
        this.endQuote = endQuote;
    }

    /**
     * Temporary feature to allow us to handle subschemas in MS SQL. We should retire this once we fully support
     * database + schema combos in Obevo.
     */
    protected void setSkipSchemaValidation(boolean skipSchemaValidation) {
        this.skipSchemaValidation = skipSchemaValidation;
    }

    /**
     * Either generate the file or directory with the DB schema information to reverse engineer given the input args,
     * or print out instructions for the user on how to generate it.
     *
     * @param out The printstream to use in the implementing function to give output to the user.
     * @param args The db args to reverse engineer.
     * @param interimDir The suggested directory to write to for interim content, if needed.
     * @return The file or directory that has the reverse-engineered content, or null if the user should instead invoke the reverse-engineering command separately
     */
    protected abstract boolean doRevengOrInstructions(PrintStream out, AquaRevengArgs args, File interimDir);

    private void revengMain(File inputPath, final AquaRevengArgs args) {
        // First, collect all files in the directory together. We will consider this as one set of objects to go through.
        final MutableList<File> files;
        if (inputPath.isFile()) {
            files = Lists.mutable.of(inputPath);
        } else {
            files = Lists.mutable.empty();
            try {
                Files.walkFileTree(inputPath.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        File fileObj = file.toFile();
                        if (fileObj.isFile()) {
                            files.add(fileObj);
                        }
                        return super.visitFile(file, attrs);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // next - extract all the objects that we've matched based on the reverse engineering inputs and the schema
        MutableList<FileProcessingContext> fileProcessingContexts = files.collect(new Function<File, FileProcessingContext>() {
            @Override
            public FileProcessingContext valueOf(File file) {
                MutableList<String> sqlSnippets = getSqlSnippets(file);

                PartitionList<Pair<String, RevengPatternOutput>> snippetPatternMatchPairs = sqlSnippets
                        .collect(patternMatchSnippet)
                        .partition(new Predicate<Pair<String, RevengPatternOutput>>() {
                            @Override
                            public boolean accept(Pair<String, RevengPatternOutput> each) {
                                RevengPatternOutput patternMatch = each.getTwo();
                                return !skipSchemaValidation
                                        && patternMatch != null
                                        && (args.isExplicitSchemaRequired() || patternMatch.getSchema() != null)
                                        && patternMatch.getSubSchema() == null
                                        && !args.getDbSchema().equalsIgnoreCase(patternMatch.getSchema());
                            }
                        });

                return new FileProcessingContext(file, snippetPatternMatchPairs);
            }
        });

        // add those pattern matches to the schema object replacer. This is there to replace all references of the schema in other objects
        final SchemaObjectReplacer schemaObjectReplacer = new SchemaObjectReplacer();
        for (FileProcessingContext fileProcessingContext : fileProcessingContexts) {
            for (Pair<String, RevengPatternOutput> snippetPatternMatchPair : fileProcessingContext.getSnippetPatternMatchPairs()) {
                schemaObjectReplacer.addPatternMatch(snippetPatternMatchPair.getTwo());
            }
        }

        final MutableList<ChangeEntry> changeEntries = fileProcessingContexts.flatCollect(new Function<FileProcessingContext, Iterable<ChangeEntry>>() {
            @Override
            public Iterable<ChangeEntry> valueOf(FileProcessingContext fileProcessingContext) {
                String schema = getObjectSchema(args.getDbSchema(), fileProcessingContext.getFile().getName());

                return revengFile(schemaObjectReplacer, fileProcessingContext.getSnippetPatternMatchPairs(), schema, args.isDebugLogEnabled());
            }
        });

//        final MutableList<ChangeEntry> invalidEntries = fileProcessingContexts.flatCollect(new Function<FileProcessingContext, Iterable<ChangeEntry>>() {
//            @Override
//            public Iterable<ChangeEntry> valueOf(FileProcessingContext fileProcessingContext) {
//                String schema = "UNMAPPEDSCHEMA";
//
//                return revengFile(schemaObjectReplacer, fileProcessingContext.getDiffSchemaSnippetPatternMatchPairs(), schema, args.isDebugLogEnabled());
//            }
//        });

        new RevengWriter().write(platform, changeEntries, new File(args.getOutputPath(), "final"), args.isGenerateBaseline(), RevengWriter.defaultShouldOverwritePredicate(), args.getJdbcUrl(), args.getDbHost(), args.getDbPort(), args.getDbServer(), args.getExcludeObjects());
    }

    /**
     * Returns the schema name to use for the given file. This implementation can vary depending on the DBMS type.
     */
    protected String getObjectSchema(String inputSchema, String fileName) {
        return inputSchema;
    }

    private static class FileProcessingContext {
        private final File file;
        private final PartitionList<Pair<String, RevengPatternOutput>> snippetPatternMatchPairs;

        FileProcessingContext(File file, PartitionList<Pair<String, RevengPatternOutput>> snippetPatternMatchPairs) {
            this.file = file;
            this.snippetPatternMatchPairs = snippetPatternMatchPairs;
        }

        File getFile() {
            return file;
        }

        List<Pair<String, RevengPatternOutput>> getSnippetPatternMatchPairs() {
            return snippetPatternMatchPairs.getRejected().toList();
        }

        List<Pair<String, RevengPatternOutput>> getDiffSchemaSnippetPatternMatchPairs() {
            return snippetPatternMatchPairs.getSelected().toList();
        }
    }

    private MutableList<ChangeEntry> revengFile(SchemaObjectReplacer schemaObjectReplacer, List<Pair<String, RevengPatternOutput>> snippetPatternMatchPairs, String inputSchema, boolean debugLogEnabled) {
        final MutableList<ChangeEntry> changeEntries = Lists.mutable.empty();

        MutableMap<String, AtomicInteger> countByObject = Maps.mutable.empty();

        int selfOrder = 0;
        String candidateObject = "UNKNOWN";
        ChangeType candidateObjectType = UnclassifiedChangeType.INSTANCE;
        for (Pair<String, RevengPatternOutput> snippetPatternMatchPair : snippetPatternMatchPairs) {
            String sqlSnippet = snippetPatternMatchPair.getOne();
            try {
                sqlSnippet = removeQuotesFromProcxmode(sqlSnippet);  // sybase ASE

                MutableMap<String, Object> debugComments = Maps.mutable.empty();

                RevengPattern chosenRevengPattern = null;
                String secondaryName = null;
                final RevengPatternOutput patternMatch = snippetPatternMatchPair.getTwo();

                debugComments.put("newPatternMatch", patternMatch != null);

                if (patternMatch != null) {
                    chosenRevengPattern = patternMatch.getRevengPattern();

                    if (chosenRevengPattern.isShouldBeIgnored()) {
                        continue;
                    }

                    debugComments.put("objectType", patternMatch.getRevengPattern().getChangeType());
                    // we add this here to allow post-processing to occur on RevengPatterns but still not define the object to write to
                    if (patternMatch.getRevengPattern().getChangeType() != null) {
                        candidateObject = patternMatch.getPrimaryName();
                        debugComments.put("originalObjectName", candidateObject);
                        candidateObject = chosenRevengPattern.remapObjectName(candidateObject);

                        debugComments.put("secondaryName", patternMatch.getSecondaryName());
                        if (patternMatch.getSecondaryName() != null) {
                            secondaryName = patternMatch.getSecondaryName();
                        }
                        if (patternMatch.getRevengPattern().getChangeType().equalsIgnoreCase(UnclassifiedChangeType.INSTANCE.getName())) {
                            candidateObjectType = UnclassifiedChangeType.INSTANCE;
                        } else {
                            candidateObjectType = platform.getChangeType(patternMatch.getRevengPattern().getChangeType());
                        }
                    }
                }

                // Ignore other schemas that may have been found in your parsing (came up during HSQLDB use case)

                sqlSnippet = schemaObjectReplacer.replaceSnippet(sqlSnippet);

                AtomicInteger objectOrder2 = countByObject.getIfAbsentPut(candidateObject, new Function0<AtomicInteger>() {
                    @Override
                    public AtomicInteger value() {
                        return new AtomicInteger(0);
                    }
                });

                if (secondaryName == null) {
                    secondaryName = "change" + objectOrder2.getAndIncrement();
                }
                RevEngDestination destination = new RevEngDestination(inputSchema, candidateObjectType, candidateObject, false);

                String annotation = chosenRevengPattern != null ? chosenRevengPattern.getAnnotation() : null;
                MutableList<Function<String, LineParseOutput>> postProcessSqls = chosenRevengPattern != null ? chosenRevengPattern.getPostProcessSqls() : Lists.mutable.<Function<String, LineParseOutput>>empty();

                for (Function<String, LineParseOutput> postProcessSql : postProcessSqls) {
                    LineParseOutput lineParseOutput = postProcessSql.valueOf(sqlSnippet);
                    sqlSnippet = lineParseOutput.getLineOutput();
                }

                Integer suggestedOrder = patternMatch != null ? patternMatch.getRevengPattern().getSuggestedOrder() : null;

                if (debugLogEnabled && debugComments.notEmpty()) {
                    String debugCommentsStr = debugComments.keyValuesView().collect(new Function<Pair<String, Object>, String>() {
                        @Override
                        public String valueOf(Pair<String, Object> object) {
                            return object.getOne() + "=" + object.getTwo();
                        }
                    }).makeString("; ");
                    sqlSnippet = "-- DEBUG COMMENT: " + debugCommentsStr + "\n" + sqlSnippet;
                }
                ChangeEntry change = new ChangeEntry(destination, sqlSnippet + "\nGO", secondaryName, annotation, ObjectUtils.firstNonNull(suggestedOrder, selfOrder++));

                postProcessChange.value(change, sqlSnippet);

                changeEntries.add(change);
            } catch (RuntimeException e) {
                throw new RuntimeException("Failed parsing on statement " + sqlSnippet, e);
            }
        }

        return changeEntries;
    }

    private final Function<String, Pair<String, RevengPatternOutput>> patternMatchSnippet = new Function<String, Pair<String, RevengPatternOutput>>() {
        @Override
        public Pair<String, RevengPatternOutput> valueOf(String sqlSnippet) {
            for (RevengPattern revengPattern : revengPatterns) {
                RevengPatternOutput patternMatch = revengPattern.evaluate(sqlSnippet);
                if (patternMatch != null) {
                    return Tuples.pair(sqlSnippet, patternMatch);
                }
            }
            return Tuples.pair(sqlSnippet, null);
        }
    };

    private class SchemaObjectReplacer {
        private final MutableSet<RevengPatternOutput> objectNames = Sets.mutable.empty();
        private final MutableSetMultimap<String, String> objectToSchemasMap = Multimaps.mutable.set.empty();
        private final MutableSetMultimap<String, String> objectToSubSchemasMap = Multimaps.mutable.set.empty();

        void addPatternMatch(RevengPatternOutput patternMatch) {
            if (patternMatch != null) {
                LOG.debug("Found object: {}", patternMatch);
                objectNames.add(patternMatch);
                if (patternMatch.getSchema() != null) {
                    objectToSchemasMap.put(patternMatch.getPrimaryName(), patternMatch.getSchema());
                }
                if (patternMatch.getSubSchema() != null) {
                    objectToSubSchemasMap.put(patternMatch.getPrimaryName(), patternMatch.getSubSchema());
                }
            }
        }

        String replaceSnippet(String sqlSnippet) {
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

                        sqlSnippet = replaceSchemaAndSubschemaInSnippet(sqlSnippet, replacerSchema, replacerSubSchema, objectOutput.getPrimaryName());
                        sqlSnippet = replaceSchemaAndSubschemaInSnippet(sqlSnippet, replacerSchema, replacerSubSchema, objectOutput.getSecondaryName());
                        sqlSnippet = replaceSchemaAndSubschemaInSnippet(sqlSnippet, replacerSchema, "", objectOutput.getPrimaryName());
                        sqlSnippet = replaceSchemaAndSubschemaInSnippet(sqlSnippet, replacerSchema, "", objectOutput.getSecondaryName());
                        sqlSnippet = replaceSchemaInSnippet(sqlSnippet, replacerSubSchema, objectOutput.getPrimaryName());
                        sqlSnippet = replaceSchemaInSnippet(sqlSnippet, replacerSubSchema, objectOutput.getSecondaryName());
                    }
                } else {
                    for (String replacerSchema : replacerSchemas) {
                        sqlSnippet = replaceSchemaInSnippet(sqlSnippet, replacerSchema, objectOutput.getPrimaryName());
                        if (objectOutput.getSecondaryName() != null) {
                            sqlSnippet = replaceSchemaInSnippet(sqlSnippet, replacerSchema, objectOutput.getSecondaryName());
                        }
                    }
                }
            }

            return sqlSnippet;
        }

        private String replaceSchemaInSnippet(String sqlSnippet, String inputSchema, String objectName) {
            for (boolean useQuotes : Lists.fixedSize.of(true, false)) {
                String sQuote = useQuotes ? startQuote : "";
                String eQuote = useQuotes ? endQuote : "";
                sqlSnippet = sqlSnippet.replaceAll(sQuote + inputSchema + "\\s*" + eQuote + "\\." + sQuote + objectName + eQuote, objectName);
            }

            return sqlSnippet;
        }

        private String replaceSchemaAndSubschemaInSnippet(String sqlSnippet, String inputSchema, String inputSubschema, String objectName) {
            for (boolean useQuotes : Lists.fixedSize.of(true, false)) {
                String sQuote = useQuotes ? startQuote : "";
                String eQuote = useQuotes ? endQuote : "";
                sqlSnippet = sqlSnippet.replaceAll(sQuote + inputSchema + "\\s*" + eQuote + "\\." + sQuote + inputSubschema + "\\s*" + eQuote + "\\." + sQuote + objectName + eQuote, objectName);
            }

            return sqlSnippet;
        }
    }

    private MutableList<String> getSqlSnippets(File file) {
        final MutableList<String> dataLines;
        dataLines = FileUtilsCobra.readLines(file);

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

        MutableList<String> sqlSnippets;
        if (stringSplitter != null) {
            String data = dataLines
                    .reject(skipLinePredicates != null ? Predicates.or(skipLinePredicates) : (Predicate) Predicates.alwaysFalse())
                    .makeString(SystemUtils.LINE_SEPARATOR);

            sqlSnippets = stringSplitter.valueOf(data);
        } else {
            // If null, then default each line to being its own parsable statement
            sqlSnippets = dataLines
                    .reject(skipLinePredicates != null ? Predicates.or(skipLinePredicates) : (Predicate) Predicates.alwaysFalse());
        }

        sqlSnippets = sqlSnippets.collect(new Function<String, String>() {
            @Override
            public String valueOf(String sqlSnippet) {
                return StringUtils.stripStart(sqlSnippet, "\r\n \t");
            }
        });
        sqlSnippets = sqlSnippets.select(StringPredicates.notBlank().and(Predicates.noneOf(skipPredicates)));
        return sqlSnippets;
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
    private String removeQuotesFromProcxmode(String input) {
        Pattern compile = Pattern.compile("sp_procxmode '(?:\")(.*?)(?:\")'", Pattern.DOTALL);

        Matcher matcher = compile.matcher(input);
        if (matcher.find()) {
            return matcher.replaceAll("sp_procxmode '" + matcher.group(1) + "'");
        } else {
            return input;
        }
    }
}
