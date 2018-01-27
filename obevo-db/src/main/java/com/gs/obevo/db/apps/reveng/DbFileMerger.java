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
import java.util.Collection;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.db.api.factory.DbPlatformConfiguration;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.util.ArgsParser;
import com.gs.obevo.util.DAStringUtil;
import com.gs.obevo.util.FileUtilsCobra;
import com.gs.obevo.util.vfs.FileObject;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.vfs2.FileType;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Lists;
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
        private int count = 0;

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

        int getCount() {
            return this.count;
        }

        void incrementCount() {
            this.count++;
        }

        ChangeType getChangeType() {
            return this.changeType;
        }

        MutableList<Pair<String, FileObject>> getFilePairs() {
            return this.filePairs;
        }

        void addFilePair(Pair<String, FileObject> filePair) {
            this.filePairs.add(filePair);
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
                    ", count=" + this.count +
                    ", distinctValuesCount=" + this.distinctValues.size() +
                    '}';
        }
    }

    public static void main(String[] argsArr) {
        DbFileMergerArgs args = new ArgsParser().parse(argsArr, new DbFileMergerArgs());
        new DbFileMerger().execute(args);
    }

    public void execute(DbFileMergerArgs args) {
        try {
            PropertiesConfiguration config = new PropertiesConfiguration(args.getDbMergeConfigFile());
            DbPlatform dialect = DbPlatformConfiguration.getInstance().valueOf(config.getString("dbType"));
            this.generateDiffs(dialect, DbMergeInfo.parseFromProperties(config), args.getOutputDir());
        } catch (ConfigurationException e) {
            throw new DeployerRuntimeException(e);
        }
    }

    private void generateDiffs(DbPlatform dialect, MutableCollection<DbMergeInfo> dbNameLocationPairs, File outputDir) {
        System.out.println("Generating diffs for " + dbNameLocationPairs);
        MultiKeyMap objectMap = new MultiKeyMap();
        for (DbMergeInfo dbNameLocationPair : dbNameLocationPairs) {
            FileObject mainDir = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject(dbNameLocationPair.getInputDir().getAbsolutePath());
            for (FileObject schemaDir : mainDir.getChildren()) {
                if (schemaDir.getType() != FileType.FOLDER) {
                    continue;
                }
                for (ChangeType changeType : dialect.getChangeTypes()) {
                    FileObject changeTypeDir = schemaDir.getChild(changeType.getDirectoryName());
                    if (changeTypeDir != null && changeTypeDir.isReadable()
                            && changeTypeDir.getType() == FileType.FOLDER) {
                        FileObject[] childFiles = changeTypeDir.getChildren();
                        for (FileObject objectFile : childFiles) {
                            if (objectFile.getType() == FileType.FILE) {
                                FileComparison fileComparison = (FileComparison) objectMap.get(changeType, objectFile
                                        .getName().getBaseName());
                                if (fileComparison == null) {
                                    fileComparison = new FileComparison(schemaDir.getName().getBaseName(),
                                            changeType, objectFile.getName().getBaseName());
                                    objectMap.put(changeType, objectFile.getName().getBaseName(), fileComparison);
                                }

                                fileComparison.addFilePair(Tuples.pair(dbNameLocationPair.getName(), objectFile));
                                String fileContent = objectFile.getStringContent();
                                String normalizedContent = DAStringUtil.normalizeWhiteSpaceFromStringOld(fileContent);
                                // modify the content here if needed
                                fileComparison.addContentValues(fileContent);
                                fileComparison.addDistinctValue(normalizedContent);
                                fileComparison.incrementCount();
                            }
                        }
                    }
                }
            }
        }

        for (FileComparison fileComparison : (Collection<FileComparison>) objectMap.values()) {
            File fileComparisonFileRoot = new File(new File(outputDir, fileComparison.getSchemaName()), fileComparison
                    .getChangeType().getDirectoryName());
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
    }
}
