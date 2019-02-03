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
package com.gs.obevo.reladomo;

import java.io.File;
import java.io.PrintStream;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.impl.util.MultiLineStringSplitter;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;

public class ReladomoDdlReveng extends AbstractDdlReveng {
    @SuppressWarnings("WeakerAccess")
    public ReladomoDdlReveng(DbPlatform dbPlatform) {
        super(dbPlatform,
                new MultiLineStringSplitter(getSplitterToken(dbPlatform), false),
                Lists.immutable.<Predicate<String>>of(
                        StringPredicates.startsWith("if exists")
                ),
                getRevengPatterns(),
                null);
    }

    /**
     * The splitter is generated differently in Reladomo per DBMS type. We have to read Reladomo code to
     * find this out (see class AbstractGeneratorDatabaseType in Reladomo).
     */
    private static String getSplitterToken(DbPlatform dbPlatform) {
        if ("ORACLE".equalsIgnoreCase(dbPlatform.getName())) {
            return "/";
        } else if ("SYBASE_ASE".equalsIgnoreCase(dbPlatform.getName())) {
            return "GO";
        } else {
            return ";";
        }
    }

    private static ImmutableList<RevengPattern> getRevengPatterns() {
        String schemaNameSubPattern = getObjectPattern("", "");
        NamePatternType namePatternType = NamePatternType.ONE;
        return Lists.immutable.with(
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+table\\s+" + schemaNameSubPattern).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES).withSuggestedOrder(-10),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + schemaNameSubPattern + "\\s+add\\s+constraint\\s+" + schemaNameSubPattern + "\\s+foreign\\s+key", 1, 2, "FK").withPostProcessSql(REMOVE_QUOTES).withSuggestedOrder(-3),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + schemaNameSubPattern + "\\s+add\\s+constraint\\s+" + schemaNameSubPattern, 1, 2, null).withPostProcessSql(REMOVE_QUOTES).withSuggestedOrder(-5),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + schemaNameSubPattern).withPostProcessSql(REMOVE_QUOTES).withSuggestedOrder(-1),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+index\\s+" + schemaNameSubPattern + "\\s+on\\s+" + schemaNameSubPattern, 2, 1, "INDEX").withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+(?:unique\\s+)?(?:\\w+\\s+)?index\\s+" + schemaNameSubPattern + "\\s+on\\s+" + schemaNameSubPattern, 2, 1, "INDEX")
        );
    }

    @Override
    protected boolean doRevengOrInstructions(PrintStream out, AquaRevengArgs args, File interimDir) {
        throw new IllegalArgumentException("Argument -inputPath must be specified");
    }
}
