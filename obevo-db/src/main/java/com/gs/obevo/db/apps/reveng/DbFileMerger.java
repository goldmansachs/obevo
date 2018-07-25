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

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.db.api.factory.DbPlatformConfiguration;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.util.ArgsParser;
import com.gs.obevo.util.DAStringUtil;
import com.gs.obevo.util.FileUtilsCobra;
import com.gs.obevo.util.vfs.FileObject;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.LegacyListDelimiterHandler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.vfs2.FileType;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;

public class DbFileMerger {
    static class FileComparison {
        private final String schemaName;
        private final String name;
        private final MutableSet<String> distinctValues = Sets.mutable.of();
        private final MutableList<String> contentValues = Lists.mutable.empty();
        private final ChangeType changeType;
        private final MutableList<Pair<String, FileObject>> filePairs = Lists.mutable.empty();
        private MutableMultimap<String, Pair<String, FileObject>> contentToEnvsMap = Multimaps.mutable.list.empty();

        FileComparison(String schemaName, ChangeType changeType, String name) {
            this.schemaName = schemaName;
            this.name = name;
            this.changeType = changeType;
        }

        String getSchemaName() {
            return this.schemaName;
        }

        String getName() {
            return this.name;
        }

        MutableSet<String> getDistinctValues() {
            return this.distinctValues;
        }

        void addDistinctValue(String distinctValue) {
            this.distinctValues.add(distinctValue);
        }

        MutableList<String> getContentValues() {
            return this.contentValues;
        }

        void addContentValues(String contentValues) {
            this.contentValues.add(contentValues);
        }

        MutableMultimap<String, Pair<String, FileObject>> getContentToEnvsMap() {
            return contentToEnvsMap;
        }

        ChangeType getChangeType() {
            return this.changeType;
        }

        MutableList<Pair<String, FileObject>> getFilePairs() {
            return this.filePairs;
        }

        void addFilePair(Pair<String, FileObject> filePair) {
            this.filePairs.add(filePair);
            String fileContent = filePair.getTwo().getStringContent();
            String normalizedContent = DAStringUtil.normalizeWhiteSpaceFromStringOld(fileContent);
            contentToEnvsMap.put(normalizedContent, filePair);
            // modify the content here if needed
//            addContentValues(fileContent);
//            addDistinctValue(normalizedContent);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FileComparison)) {
                return false;
            }

            FileComparison that = (FileComparison) o;

            if (this.changeType != that.changeType) {
                return false;
            }
            if (!this.name.equals(that.name)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = this.name.hashCode();
            result = 31 * result + this.changeType.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "FileComparison{" +
                    "changeType=" + this.changeType +
                    ", name='" + this.name + '\'' +
                    ", distinctValuesCount=" + this.distinctValues.size() +
                    '}';
        }
    }

    public static void main(String[] argsArr) {
        DbFileMergerArgs args = new ArgsParser().parse(argsArr, new DbFileMergerArgs());
        new DbFileMerger().execute(args);
    }

    public void execute(DbFileMergerArgs args) {
        PropertiesConfiguration config;
        RichIterable<DbMergeInfo> dbNameLocationPairs;
        try {
            config = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                    .configure(new Parameters().properties()
                            .setFile(args.getDbMergeConfigFile())
                            .setListDelimiterHandler(new LegacyListDelimiterHandler(','))
                    )
                    .getConfiguration();
            dbNameLocationPairs = DbMergeInfo.parseFromProperties(config);
        } catch (Exception e) {
            throw new DeployerRuntimeException("Exception reading configs from file " + args.getDbMergeConfigFile(), e);
        }

        DbPlatform dialect = DbPlatformConfiguration.getInstance().valueOf(config.getString("dbType"));
        this.generateDiffs(dialect, dbNameLocationPairs, args.getOutputDir());
    }

    private void generateDiffs(DbPlatform dialect, RichIterable<DbMergeInfo> dbNameLocationPairs, File outputDir) {
        System.out.println("Generating diffs for " + dbNameLocationPairs);
        MutableMap<Pair<ChangeType, String>, FileComparison> objectMap = Maps.mutable.empty();
        for (DbMergeInfo dbNameLocationPair : dbNameLocationPairs) {
            FileObject mainDir = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject(dbNameLocationPair.getInputDir().getAbsolutePath());
            for (final FileObject schemaDir : mainDir.getChildren()) {
                if (schemaDir.getType() != FileType.FOLDER) {
                    continue;
                }
                for (final ChangeType changeType : dialect.getChangeTypes()) {
                    FileObject changeTypeDir = schemaDir.getChild(changeType.getDirectoryName());
                    if (changeTypeDir != null && changeTypeDir.isReadable()
                            && changeTypeDir.getType() == FileType.FOLDER) {
                        FileObject[] childFiles = changeTypeDir.getChildren();
                        for (final FileObject objectFile : childFiles) {
                            if (objectFile.getType() == FileType.FILE) {
                                final String objectName = FilenameUtils.removeExtension(objectFile.getName().getBaseName());
                                FileComparison fileComparison = objectMap.getIfAbsentPut(Tuples.pair(changeType, objectName), new Function0<FileComparison>() {
                                    @Override
                                    public FileComparison value() {
                                        return new FileComparison(schemaDir.getName().getBaseName(),
                                                changeType, objectName);
                                    }
                                });

                                fileComparison.addFilePair(Tuples.pair(dbNameLocationPair.getName(), objectFile));
                            }
                        }
                    }
                }
            }
        }

        for (Pair<Pair<ChangeType, String>, FileComparison> comparisonPair : objectMap.keyValuesView()) {
            ChangeType changeType = comparisonPair.getOne().getOne();
            final String objectName = comparisonPair.getOne().getTwo();
            FileComparison fileComparison = comparisonPair.getTwo();
            boolean onlyOneDistinctValue = fileComparison.getContentToEnvsMap().sizeDistinct() == 1;
            boolean instancesMissing = fileComparison.getContentToEnvsMap().size() != dbNameLocationPairs.size();

            String metadataMissingSuffix;
            if (instancesMissing) {
                MutableSet<String> allInstancse = comparisonPair.getTwo().contentToEnvsMap.valuesView().collect(Functions.<String>firstOfPair()).toSet();
                SetIterable<String> instanceNames = dbNameLocationPairs.collect(new Function<DbMergeInfo, String>() {
                    @Override
                    public String valueOf(DbMergeInfo object) {
                        return object.getName();
                    }
                }).toSet();
                metadataMissingSuffix = " comment=\"missingInInstances_" + instanceNames.difference(allInstancse).toSortedList().makeString(",") + "\"";
            } else {
                metadataMissingSuffix = "";
            }

            File fileComparisonFileRoot = new File(new File(outputDir, fileComparison.getSchemaName()), changeType.getDirectoryName());
            int index = 0;
            for (RichIterable<Pair<String, FileObject>> fileComparisonPairs : fileComparison.getContentToEnvsMap().multiValuesView()) {
                // one element per distinct file

                Pair<String, FileObject> fileComparisonPair = fileComparisonPairs.getFirst();
                String instanceName = fileComparisonPair.getOne();
                String fileContent = fileComparisonPair.getTwo().getStringContent();

                File outputFile;
                if (onlyOneDistinctValue) {
                    if (instancesMissing) {
                        outputFile = new File(fileComparisonFileRoot, objectName + ".instancesMissing." + fileComparisonPair.getTwo().getName().getExtension());
                    } else {
                        outputFile = new File(fileComparisonFileRoot, fileComparison.getName());
                    }
                } else {
                    outputFile = new File(fileComparisonFileRoot, objectName + "." + (index++) + "." + fileComparisonPair.getTwo().getName().getExtension());
                }

                String metadataPrefix;
                if (!onlyOneDistinctValue || !metadataMissingSuffix.isEmpty()) {
                    SetIterable<String> dbNames = fileComparisonPairs.collect(Functions.<String>firstOfPair()).toSet();
                    metadataPrefix = "//// METADATA includeEnvs=\"" + dbNames.toSortedList().collect(StringFunctions.append("*")).makeString(",") + "\""
                            + metadataMissingSuffix + "\n";
                } else {
                    metadataPrefix = "";
                }

                FileUtilsCobra.writeStringToFile(outputFile, metadataPrefix + fileContent);
            }
        }

/*
        for (FileComparison fileComparison : objectMap.values()) {

            if (fileComparison.getDistinctValues().size() == 1) {
                File outputFile;
                if (fileComparison.getCount() == dbNameLocationPairs.size()) {
                    outputFile = new File(fileComparisonFileRoot, fileComparison.getName());
                } else {
                    MutableList<String> dbNames = fileComparison.getFilePairs().collect(
                            Functions.<String>firstOfPair());
                    String dbNameString = "only-" + dbNames.sortThis().makeString("-");
                    File dbDir = new File(fileComparisonFileRoot, dbNameString);
                    outputFile = new File(dbDir, fileComparison.getName());

                    File packageInfoFile = new File(dbDir, "package-info.txt");
                    FileUtilsCobra.writeStringToFile(packageInfoFile, "//// METADATA includeEnvs=\""
                            + dbNames.sortThis().collect(StringFunctions.append("*")).makeString(",") + "\"");
                }
                FileUtilsCobra.writeStringToFile(outputFile, fileComparison.getContentValues().getFirst());
            } else {
                for (Pair<String, FileObject> dbNameFileObjectPair : fileComparison.getFilePairs()) {
                    String dbName = dbNameFileObjectPair.getOne();
                    File outputFile = new File(new File(fileComparisonFileRoot, dbName), fileComparison.getName());
                    File packageInfoFile = new File(new File(fileComparisonFileRoot, dbName), "package-info.txt");

                    String fileContent = dbNameFileObjectPair.getTwo().getStringContent();
                    FileUtilsCobra.writeStringToFile(outputFile, fileContent);
                    FileUtilsCobra.writeStringToFile(packageInfoFile, "//// METADATA includeEnvs=\""
                            + StringFunctions.append("*").valueOf(dbName) + "\"");
                }
            }
        }
*/

    }
}
