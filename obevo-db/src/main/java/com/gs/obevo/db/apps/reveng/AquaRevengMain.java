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
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.impl.core.util.MultiLineStringSplitter;
import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.RuleBinding;
import com.gs.obevo.impl.changetypes.UnclassifiedChangeType;
import com.gs.obevo.util.FileUtilsCobra;
import com.gs.obevo.util.inputreader.Credential;
import com.gs.obevo.util.inputreader.CredentialReader;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.partition.list.PartitionMutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
import org.eclipse.collections.impl.tuple.Tuples;

public class AquaRevengMain {
    private static final CredentialReader credentialReader = new CredentialReader();
    public static final FileFilter VCS_FILE_FILTER = FileFilterUtils.makeCVSAware(
            FileFilterUtils.makeSVNAware(
                    FileFilterUtils.trueFileFilter()));

    private boolean tablespaceToken;
    private boolean preprocessSchemaTokens;
    /**
     * @deprecated
     */
    @Deprecated
    private boolean tokenizeDefaultSchema;
    private boolean generateBaseline;
    private Multimap<String, RuleBinding> ruleBindingMap = FastListMultimap.newMultimap();
    private File workDir;

    public void execute(AquaRevengArgs args) {
        RevengMode mode = args.getMode();
        this.workDir = FileUtilsCobra.createTempDir("deploy-aquareveng");
        switch (mode) {
        case DATA:
            CsvStaticDataWriter.start(args, this.workDir);
            break;
        case SCHEMA:
            this.tablespaceToken = args.getTablespaceToken();
            this.tokenizeDefaultSchema = args.getTokenizeDefaultSchema();
            this.preprocessSchemaTokens = args.isPreprocessSchemaTokens();
            this.generateBaseline = args.isGenerateBaseline();
            this.doExecute(args);
            break;
        default:
            throw new IllegalArgumentException("No other mode supported for reveng: " + mode);
        }
    }

    private void doExecute(final AquaRevengArgs args) {
        File input = args.getInputDir();
        File outputDir = args.getOutputDir();

        System.out.println("Performing reverse engineering on input " + input);

        Validate.notNull(args.getDbSchema(), "dbSchema argument must be specified");
        Validate.notNull(input, "Directory " + input + " was not found"); // Maven supplies a null File value in this case

        if (args.getDbHost() != null) {
            DbEnvironment env = new DbEnvironment();
            env.setPlatform(args.getDbPlatform());
            env.setSystemDbPlatform(args.getDbPlatform());
            env.setDbHost(args.getDbHost());
            env.setDbPort(args.getDbPort());
            env.setDbServer(args.getDbServer());
            if (args.getDriverClass() != null) {
                env.setDriverClassName(args.getDriverClass());
            }
            Schema schema = new Schema(args.getDbSchema());
            env.setSchemas(Sets.immutable.with(schema));

            Credential credential = credentialReader.getCredential(args.getUsername(), args.getPassword(), false, null, null,
                    null);

            final DbDeployerAppContext ctxt = env.getAppContextBuilder().setCredential(credential).setWorkDir(workDir).buildDbContext();

            // Aqua Data Studio can't extract ASE rule bindings, so we do it ourselves
            DaCatalog database = ctxt.getDbMetadataManager().getDatabase(args.getDbSchema(), new DaSchemaInfoLevel().setRetrieveRuleBindings(true), false, false);
            ImmutableCollection<RuleBinding> ruleBindings = database.getRuleBindings();

            this.ruleBindingMap = ruleBindings.groupBy(RuleBinding.TO_OBJECT);
        }

        // Sorting it so that the reverse engineering output can be predicatable (good for consistency and unit tests)
        MutableList<File> files = Lists.mutable.with(input.listFiles(VCS_FILE_FILTER)).sortThisBy(new Function<File, Comparable>() {
            @Override
            public Comparable valueOf(File file) {
                return file.getName();
            }
        });

        if (preprocessSchemaTokens && !tablespaceToken) {
            files = preprocessSchemaTokens(files, args.getDbSchema(), new File(outputDir, "interim-schemaReplaced"), args.getDbPlatform());
        }

        this.patternMap = initPatternMap(args.getDbPlatform());
        MutableList<ChangeEntry> allRevEngDestinations = Lists.mutable.empty();
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println("Skipping " + file + " as this is a directory; not expecting this in the format");
                continue;
            } else if (file.getName().equals(CsvStaticDataWriter.STATIC_DATA_TABLES_FILE_NAME)) {
                System.out.println("Skipping the static data table file: " + file);
                continue;
            }
            allRevEngDestinations.addAll(this.calculateRevEngDest(args.getDbPlatform(), file, args.getNameCombinePattern()));
        }

        File outputWriteFolder = tablespaceToken ? outputDir : new File(outputDir, "final");  // check the tablespaceToken value for backwards-compatibility
        new RevengWriter().write(args.getDbPlatform(), allRevEngDestinations, outputWriteFolder, this.generateBaseline, null, args.getJdbcUrl(), args.getDbHost(), args.getDbPort(), args.getDbServer(), args.getExcludeObjects());
    }

    private ImmutableMap<ChangeType, Pattern> initPatternMap(Platform platform) {
        MutableMap<String, Pattern> params = Maps.mutable.<String, Pattern>with()
                .withKeyValue(ChangeType.SP_STR, Pattern.compile("(?i)create\\s+proc(?:edure)?\\s+(\\w+)", Pattern.DOTALL))
                .withKeyValue(ChangeType.FUNCTION_STR, Pattern.compile("(?i)create\\s+func(?:tion)?\\s+(\\w+)", Pattern.DOTALL))
                .withKeyValue(ChangeType.VIEW_STR, Pattern.compile("(?i)create\\s+view\\s+(\\w+)", Pattern.DOTALL))
                .withKeyValue(ChangeType.SEQUENCE_STR, Pattern.compile("(?i)create\\s+seq(?:uence)?\\s+(\\w+)", Pattern.DOTALL))
                .withKeyValue(ChangeType.TABLE_STR, Pattern.compile("(?i)create\\s+table\\s+(\\w+)", Pattern.DOTALL))
                .withKeyValue(ChangeType.DEFAULT_STR, Pattern.compile("(?i)create\\s+default\\s+(\\w+)", Pattern.DOTALL))
                .withKeyValue(ChangeType.RULE_STR, Pattern.compile("(?i)create\\s+rule\\s+(\\w+)", Pattern.DOTALL))
                .withKeyValue(ChangeType.USERTYPE_STR, Pattern.compile("(?i)^\\s*sp_addtype\\s+", Pattern.DOTALL))
                .withKeyValue(ChangeType.INDEX_STR, Pattern.compile("(?i)create\\s+(?:unique\\s+)?(?:\\w+\\s+)?index\\s+\\w+\\s+on\\s+(\\w+)", Pattern.DOTALL))
        ;

        MutableMap<ChangeType, Pattern> patternMap = Maps.mutable.<ChangeType, Pattern>with();
        for (String changeTypeName : params.keysView()) {
            if (platform.hasChangeType(changeTypeName)) {
                ChangeType changeType = platform.getChangeType(changeTypeName);
                patternMap.put(changeType, params.get(changeTypeName));
            }
        }

        return patternMap.toImmutable();
    }

    private MutableList<File> preprocessSchemaTokens(MutableList<File> files, String dbSchema, final File interimFolder, DbPlatform dbPlatform) {
        // adding DBO to help w/ Sybase ASE; we should make this code more polymorphic
        String schemaSeparatorRegex = dbPlatform.isSubschemaSupported() ? "\\.(?:dbo)?\\." : "\\.";

        final Pattern dbSchemaPattern = Pattern.compile(String.format("(?i)%1$s%2$s(\\w+)", dbSchema, schemaSeparatorRegex));
        return files.collect(new Function<File, File>() {
            @Override
            public File valueOf(File file) {
                String fileContent = FileUtilsCobra.readFileToString(file);
                final Matcher matcher = dbSchemaPattern.matcher(fileContent);
                StringBuffer sb = new StringBuffer(fileContent.length());

                while (matcher.find()) {
                    matcher.appendReplacement(sb, matcher.group(1));
                }
                matcher.appendTail(sb);

                File tempFile = new File(interimFolder, file.getName());
                FileUtilsCobra.writeStringToFile(tempFile, sb.toString());
                return tempFile;
            }
        });
    }

    private ImmutableMap<ChangeType, Pattern> patternMap;

    private static Pair<Integer, String> getStartIndex(String str, Pattern p) {
        Matcher m = p.matcher(str);
        while (m.find()) {
            String objectName = m.groupCount() > 0 ? m.group(1) : null;  // by convention, the second group collected has the name
            return Tuples.pair(m.start(), objectName);
        }
        return Tuples.pair(Integer.MAX_VALUE, null);
    }

    static String extractName(String objectName, String nameCombinePattern) {
        if (nameCombinePattern != null) {
            String patternStr = nameCombinePattern.replace("{}", "(.*)");
            Pattern namePattern = Pattern.compile(patternStr);
            Matcher nameMatcher = namePattern.matcher(objectName);
            if (nameMatcher.matches()) {
                return nameMatcher.group(1);
            }
        }

        return objectName;
    }

    private MutableList<ChangeEntry> calculateRevEngDest(DbPlatform dbPlatform, File file, String nameCombinePattern) {
        String[] fileNameParts = file.getName().split("\\.");
        String wholeFileString = FileUtilsCobra.readFileToString(file);
        String schema = fileNameParts[0];
        if (fileNameParts.length != 3) {
            if (!schema.equals("dbo") || fileNameParts.length != 5) {
                return Lists.mutable.with(new ChangeEntry(new RevEngDestination(schema, UnclassifiedChangeType.INSTANCE,
                        file.getName(), false), wholeFileString, "n/a", null, 0));
            }
        }
        //public RevEngDestination(String content, ChangeType dbObjectType, String schema, String objectName,
        //boolean duplicate, int order) {



        ChangeTypeInfo contentObjectInfo = this.determineChangeType(wholeFileString);
        ChangeType objType = contentObjectInfo.getChangeType();
        String fileObjectName = this.getObjectNameFromFilename(fileNameParts[1], objType);

        // This is for cases in Sybase where teams had backed up db objects, and so it was created w/ a different name
        // in the object, but the underlying content points to the original name
        // We only fire off this check in case the scan of the content does pick up the object name, as to be
        // conservative
        // Addendum on 2014-10-15 - in case we know an object must be combined w/ another (e.g. index to parent),
        // we don't consider the object diff there as a duplicate
        boolean duplicate = dbPlatform.isDuplicateCheckRequiredForReverseEngineering() && !objType.getName().equals(ChangeType.INDEX_STR) && contentObjectInfo.getObjectName() != null &&
                !contentObjectInfo.getObjectName().equalsIgnoreCase(fileObjectName);

        String objectName = contentObjectInfo.getObjectName() != null
                && !duplicate ? contentObjectInfo.getObjectName() : fileObjectName;

        String originalObjectName = objectName;

        objectName = extractName(objectName, nameCombinePattern);

        RevEngDestination dest = new RevEngDestination(schema, contentObjectInfo.getChangeType(),
                objectName, duplicate);

        // tokenize default schema name in the objects - this is only done if -tokenizeDefaultSchema is specified
        // on the command line
        if (this.tokenizeDefaultSchema) {
            wholeFileString = this.tokenizeDefaultSchema(schema, wholeFileString, objType, objectName);
        }

        MutableList<ChangeEntry> changes = Lists.mutable.empty();
        // handle any special overriding use cases if needed (currently not needed for view or sequence)
        if (objType.getName().equals(ChangeType.TABLE_STR)) {
            MutableList<String> statements = MultiLineStringSplitter.createSplitterOnSpaceAndLine("GO").valueOf(wholeFileString);

            // append in tablespace if needed
            if (this.tablespaceToken) {
                statements.set(0, statements.get(0) + " IN ${" + schema.toUpperCase() + "_TABLESPACE" + "}");
            }

            PartitionMutableList<String> splitStatements = statements.partition(StringPredicates
                    .contains("FOREIGN KEY"));
            MutableList<String> fkStatements = splitStatements.getSelected();
            final Pattern triggerPattern = Pattern.compile("(?i).*create.*trigger\\s+(\\w+)\r?\n.*", Pattern.DOTALL);
            splitStatements = splitStatements.getRejected().partition(new Predicate<String>() {
                @Override
                public boolean accept(String s) {
                    return triggerPattern.matcher(s).matches();
                }
            });
            MutableList<String> triggerStatements = splitStatements.getSelected();

            final Pattern indexPattern = Pattern.compile("(?i).*create.*index\\s+(\\w+)\r?\n.*", Pattern.DOTALL);
            splitStatements = splitStatements.getRejected().partition(new Predicate<String>() {
                @Override
                public boolean accept(String s) {
                    Matcher m = indexPattern.matcher(s);
                    return m.matches();
                }
            });
            MutableList<String> indexStatements = splitStatements.getSelected();
            MutableList<String> nonFkStatements = splitStatements.getRejected();

            int selfOrder = 0;
            String endingString;
            if (nonFkStatements.isEmpty() || nonFkStatements.getLast().endsWith("GO") || StringUtils.isBlank(nonFkStatements.getLast())) {
                endingString = "\n";
            } else {
                endingString = "\nGO\n";
            }

            changes.add(new ChangeEntry(dest, nonFkStatements.makeString("\n", "\nGO\n", endingString), "init", null,
                    selfOrder++));

            if (!fkStatements.isEmpty()) {
                changes.add(new ChangeEntry(dest,
                        fkStatements.collect(StringFunctions.trim()).makeString("", "\nGO\n", "\nGO\n"), "initFk",
                        "FK", selfOrder++));
            }
            for (String index : indexStatements) {
                Matcher m = indexPattern.matcher(index);
                if (m.matches()) {
                    String indexName = m.group(1);
                    changes.add(new ChangeEntry(dest, index.trim() + "\nGO", indexName, "INDEX", selfOrder++));
                } else {
                    throw new IllegalStateException("Invalid state - this should have already had the index in it: "
                            + index);
                }
            }
            for (String trigger : triggerStatements) {
                Matcher m = triggerPattern.matcher(trigger);
                if (m.matches()) {
                    String name = m.group(1);
                    changes.add(new ChangeEntry(dest, trigger.trim() + "\nGO", name, "TRIGGER", selfOrder++));
                } else {
                    throw new IllegalStateException(
                            "Invalid state - this should have already had the trigger name in it: " + trigger);
                }
            }

            RichIterable<RuleBinding> ruleBindings = this.ruleBindingMap.get(objectName);
            if (ruleBindings != null) {
                int i = 1;
                for (RuleBinding binding : ruleBindings) {
                    changes.add(new ChangeEntry(dest, binding.getSql().trim(), "binding" + i++, "FK", selfOrder++));
                }
            }
        } else if (objType.getName().equals(ChangeType.INDEX_STR)) {
            RevEngDestination newDest = new RevEngDestination(dest.getSchema(), dbPlatform.getChangeType(ChangeType.TABLE_STR),
                    dest.getObjectName(), dest.isDuplicate());
            // the fileObjectName.replace line is to replace the file name w/ the object name in case it differs (e.g
            // . coalescing the different table files into one)
            changes.add(new ChangeEntry(newDest, wholeFileString.trim()
                    , "index_" + fileObjectName.replace(originalObjectName, objectName)
                    , "INDEX", 100));
//        } else if (objType == ChangeType.TRIGGER) {
//
//            RevEngDestination newDest = new RevEngDestination(dest.getSchema(), ChangeType.TABLE,
//                    dest.getObjectName(), dest.isDuplicate());
//            // the fileObjectName.replace line is to replace the file name w/ the object name in case it differs (e.g
//            // . coalescing the different table files into one)
//            changes.add(new ChangeEntry(newDest, wholeFileString.trim()
//                    , "trigger_" + fileObjectName.replace(originalObjectName, objectName)
//                    , "TRIGGER", 200));
        } else {
            RichIterable<RuleBinding> ruleBindings = this.ruleBindingMap.get(objectName);
            if (ruleBindings != null && !ruleBindings.isEmpty()) {
                for (RuleBinding binding : ruleBindings) {
                    wholeFileString = wholeFileString + "\nGO\n" + binding.getSql().trim() + "\nGO";
                }
            }

            changes.add(new ChangeEntry(dest, wholeFileString));
        }

        dbPlatform.postProcessChangeForRevEng(changes.getFirst(), wholeFileString);

        return changes;
    }

    private String tokenizeDefaultSchema(String schema, String content, ChangeType objType, String objectName) {
        ImmutableSet<String> tokenizableChangeTypes = Sets.immutable.with(ChangeType.FUNCTION_STR, ChangeType.SP_STR, ChangeType.VIEW_STR, UnclassifiedChangeType.INSTANCE.getName());

        // only tokenize the schema name for functions, sps, views, and other
        if (tokenizableChangeTypes.contains(objType.getName())) {
            File tempFile;
            try {
                // ensure the prefix is at least 3 chars long. Otherwise it will fail
                String padName = "";
                if (objectName.length() < 3) {
                    padName = "123";
                }
                tempFile = File.createTempFile(objectName + padName, ".sql");
            } catch (IOException e) {
                throw new RuntimeException("failed creating a temp file for " + objectName, e);
            }
            System.out.println("using temp file " + tempFile);
            // tokenize the default schema
            String tokenName = "${" + schema.toLowerCase() + ".token}";

            content = content.replace(schema.toLowerCase() + ".", tokenName + ".");
            content = content.replace(schema.toUpperCase() + ".", tokenName + ".");

            FileUtilsCobra.writeStringToFile(tempFile, content);

            return content;
        }
        return content;
    }

    private String getObjectNameFromFilename(String fname, ChangeType objType) {
        if (!objType.getName().equals(ChangeType.SP_STR) && !objType.getName().equals(ChangeType.FUNCTION_STR)) {
            return fname;
        }
        String result = fname;
        // only for db2 for specific-names bit
        int hyphenIndex = fname.lastIndexOf('-');
        if (hyphenIndex != -1) {
            // use the name to the right of the hyphen - ensure it doesn't start with SQL
            // if it does, use what is on the right side
            result = fname.substring(hyphenIndex + 1);
            if (result.indexOf("SQL") == 0) {
                result = fname.substring(0, hyphenIndex);
            }
        }
        return result;
    }

    private ChangeTypeInfo determineChangeType(final String wholeFileString) {
        RichIterable<ChangeTypeInfo> changeTypeInfos = this.patternMap.keyValuesView().collect(
                new Function<Pair<ChangeType, Pattern>, ChangeTypeInfo>() {
                    @Override
                    public ChangeTypeInfo valueOf(Pair<ChangeType, Pattern> object) {
                        Pair<Integer, String> contentInfo = getStartIndex(wholeFileString, object.getTwo());
                        return new ChangeTypeInfo(object.getOne()
                                , contentInfo.getOne()
                                , contentInfo.getTwo()
                        );
                    }
                });
        ChangeTypeInfo chosenChangeTypeInfo = changeTypeInfos.minBy(ChangeTypeInfo.TO_START_INDEX);

        if (chosenChangeTypeInfo.getStartIndex() == Integer.MAX_VALUE) {
            return new ChangeTypeInfo(UnclassifiedChangeType.INSTANCE, Integer.MAX_VALUE, null);
        } else {
            return chosenChangeTypeInfo;
        }
    }

    private static class ChangeTypeInfo {
        private final ChangeType changeType;
        private final int startIndex;
        private final String objectName;

        public static final Function<ChangeTypeInfo, Integer> TO_START_INDEX = new Function<ChangeTypeInfo, Integer>() {
            @Override
            public Integer valueOf(ChangeTypeInfo object) {
                return object.getStartIndex();
            }
        };

        private ChangeTypeInfo(ChangeType changeType, int startIndex, String objectName) {
            this.changeType = changeType;
            this.startIndex = startIndex;
            this.objectName = objectName;
        }

        public ChangeType getChangeType() {
            return this.changeType;
        }

        public int getStartIndex() {
            return this.startIndex;
        }

        public String getObjectName() {
            return this.objectName;
        }
    }
}
