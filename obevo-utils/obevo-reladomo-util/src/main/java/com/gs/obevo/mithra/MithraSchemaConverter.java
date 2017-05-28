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
package com.gs.obevo.mithra;

import java.io.File;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.db.api.factory.DbPlatformConfiguration;
import com.gs.obevo.db.apps.reveng.AquaRevengMain;
import com.gs.obevo.db.apps.reveng.ChangeEntry;
import com.gs.obevo.db.apps.reveng.RevEngDestination;
import com.gs.obevo.db.apps.reveng.RevengWriter;
import com.gs.obevo.util.DAStringUtil;
import com.gs.obevo.util.FileUtilsCobra;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MithraSchemaConverter {
    private static final Logger LOG = LoggerFactory.getLogger(MithraSchemaConverter.class);

    public void convertMithraDdlsToDaFormat(MithraSchemaConverterArgs args) {
        convertMithraDdlsToDaFormat(DbPlatformConfiguration.getInstance().valueOf(args.getPlatform()), args.getInputDir(), args.getOutputDir(), args.getDbSchema(), !args.isDontGenerateBaseline());
    }

    public void convertMithraDdlsToDaFormat(final Platform platform, File inputDir, File outputDir, final String schemaName, boolean generateBaseline) {
        if (!inputDir.canRead()) {
            throw new IllegalArgumentException("Cannot read input directory: " + inputDir);
        }
        final MutableList<ChangeEntry> changeEntries = ArrayAdapter.adapt(inputDir.listFiles(AquaRevengMain.VCS_FILE_FILTER)).flatCollect(new Function<File, Iterable<ChangeEntry>>() {
            @Override
            public Iterable<ChangeEntry> valueOf(File file) {
                final String objectName = FilenameUtils.getBaseName(file.getName());
                final String fileExtension = FilenameUtils.getExtension(file.getName());

                final Function<Pair<String, Integer>, ChangeEntry> func;
                if (fileExtension.equalsIgnoreCase("ddl")) {
                    func = new Function<Pair<String, Integer>, ChangeEntry>() {
                        @Override
                        public ChangeEntry valueOf(Pair<String, Integer> sqlIndexPair) {
                            return getTableChange(platform, schemaName, objectName, sqlIndexPair.getOne(), sqlIndexPair.getTwo());
                        }
                    };
                } else if (fileExtension.equalsIgnoreCase("fk")) {
                    func = new Function<Pair<String, Integer>, ChangeEntry>() {
                        @Override
                        public ChangeEntry valueOf(Pair<String, Integer> sqlIndexPair) {
                            return getFkChange(platform, schemaName, objectName, sqlIndexPair.getOne(), sqlIndexPair.getTwo());
                        }
                    };
                } else if (fileExtension.equalsIgnoreCase("idx")) {
                    func = new Function<Pair<String, Integer>, ChangeEntry>() {
                        @Override
                        public ChangeEntry valueOf(Pair<String, Integer> sqlIndexPair) {
                            return getIndexChange(platform, schemaName, objectName, sqlIndexPair.getOne(), sqlIndexPair.getTwo());
                        }
                    };
                } else {
                    LOG.warn("Unexpected file extension {} for file {}, so we are not processing this file. Expecting extensions: .ddl, .fx, .idx", fileExtension, file);
                    // unexpected
                    return Lists.mutable.empty();
                }

                final RichIterable<String> sqls = splitSqlBySemicolon(FileUtilsCobra.readFileToString(file));
                return sqls.asLazy().reject(DAStringUtil.STRING_IS_BLANK).zipWithIndex().collect(func);
            }
        });

        new RevengWriter().write(platform, changeEntries, outputDir, generateBaseline, RevengWriter.defaultShouldOverwritePredicate(), null, null, null, null);
    }

    /*
     * public for testing
     */
    static ListIterable<String> splitSqlBySemicolon(String sql) {
        return ArrayAdapter.adapt(sql.split(";(\n|$|\r\n)"));
    }

    private ChangeEntry getTableChange(Platform platform, String schemaName, String objectName, String sql, int index) {
        return new ChangeEntry(new RevEngDestination(schemaName, platform.getChangeType(ChangeType.TABLE_STR), objectName, false), sql, "\"init-table\"", null, 0);
    }
    private ChangeEntry getIndexChange(Platform platform, String schemaName, String objectName, String sql, int index) {
        return new ChangeEntry(new RevEngDestination(schemaName, platform.getChangeType(ChangeType.TABLE_STR), objectName, false), sql, "\"index" + index + "\"", "INDEX", index + 1);
    }
    private ChangeEntry getFkChange(Platform platform, String schemaName, String objectName, String sql, int index) {
        return new ChangeEntry(new RevEngDestination(schemaName, platform.getChangeType(ChangeType.TABLE_STR), objectName, false), sql, "\"fk" + index + "\"", "FK", index + 1);
    }
}
