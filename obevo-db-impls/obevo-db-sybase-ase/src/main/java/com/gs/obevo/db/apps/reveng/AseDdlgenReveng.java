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
import java.io.PrintStream;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.apps.reveng.RevengPattern.NamePatternType;
import com.gs.obevo.db.impl.platforms.sybasease.AseDbPlatform;
import com.gs.obevo.impl.reader.TextMarkupDocumentReader;
import com.gs.obevo.impl.util.MultiLineStringSplitter;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;

public class AseDdlgenReveng extends AbstractDdlReveng {
    public AseDdlgenReveng() {
        super(
                new AseDbPlatform(),
                MultiLineStringSplitter.createSplitterOnSpaceAndLine("go"),
                Lists.immutable.<Predicate<String>>of(
                        StringPredicates.startsWith("-- Sybase Adaptive Server Enterprise DDL Generator Utility"),
                        StringPredicates.startsWith("use "),
                        StringPredicates.startsWith("IF EXISTS ("),
                        StringPredicates.startsWith("create database"),
                        StringPredicates.startsWith("------------------------------------------------------------"),
                        StringPredicates.startsWith("Grant "),
                        StringPredicates.startsWith("exec sp_addgroup"),
                        StringPredicates.startsWith("exec sp_adduser"),
                        StringPredicates.startsWith("setuser"),
                        StringPredicates.startsWith("SETUSER"),
                        StringPredicates.startsWith("set quoted_identifier"),
                        StringPredicates.startsWith("sp_placeobject"),
                        StringPredicates.startsWith("exec sp_changedbowner"),
                        StringPredicates.startsWith("exec master.dbo.sp_dboption"),
                        StringPredicates.startsWith("checkpoint"),
                        StringPredicates.startsWith("sp_addthreshold"),
                        StringPredicates.startsWith("exec sp_addalias"),
                        StringPredicates.startsWith("-- DDLGen Completed")),
                getRevengPatterns(),
                new Procedure2<ChangeEntry, String>() {
                    @Override
                    public void value(ChangeEntry changeEntry, String sql) {
                        if (sql.contains("\"")) {
                            changeEntry.addMetadataAnnotation(TextMarkupDocumentReader.TOGGLE_DISABLE_QUOTED_IDENTIFIERS);
                        }
                    }
                }
        );
    }

    private static ImmutableList<RevengPattern> getRevengPatterns() {
        String nameSubPattern = getCatalogSchemaObjectPattern("", "");
        NamePatternType namePatternType = RevengPattern.NamePatternType.THREE;
        return Lists.immutable.with(
                new RevengPattern(ChangeType.SEQUENCE_STR, namePatternType, "(?i)create\\s+seq(?:uence)?\\s+" + nameSubPattern),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+table\\s+" + nameSubPattern),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + nameSubPattern + "\\s+add\\s+constraint\\s+" + nameSubPattern + "\\s+foreign\\s+key", 1, 2, "FK"),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + nameSubPattern + "\\s+add\\s+constraint\\s+" + nameSubPattern, 1, 2, null),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + nameSubPattern),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+(?:unique\\s+)?(?:\\w+\\s+)?index\\s+" + nameSubPattern + "\\s+on\\s+" + nameSubPattern, 2, 1, "INDEX"),
                new RevengPattern(ChangeType.FUNCTION_STR, namePatternType, "(?i)create\\s+func(?:tion)?\\s+" + nameSubPattern),
                new RevengPattern(ChangeType.VIEW_STR, namePatternType, "(?i)create\\s+view\\s+" + nameSubPattern),
                new RevengPattern(ChangeType.SP_STR, namePatternType, "(?i)create\\s+proc(?:edure)?\\s+" + nameSubPattern),
                new RevengPattern(ChangeType.TRIGGER_STR, namePatternType, "(?i)create\\s+trigger\\s+" + nameSubPattern),
                new RevengPattern(ChangeType.DEFAULT_STR, namePatternType, "(?i)create\\s+default\\s+" + nameSubPattern),
                new RevengPattern(ChangeType.RULE_STR, namePatternType, "(?i)create\\s+rule\\s+" + nameSubPattern),
                new RevengPattern(ChangeType.USERTYPE_STR, namePatternType, "(?i)^(?:exec\\s+)?sp_addtype\\s+'" + nameSubPattern + "'")
        );
    }

    @Override
    protected boolean doRevengOrInstructions(PrintStream out, AquaRevengArgs args, File interimDir) {
        out.println("1) Run the following command to generate the DDL file:");
        out.println(getCommandWithDefaults(args, "<username>", "<password>", "<dbHost>", "<dbPortNumber>", "<dbSchema>", "<outputFile>"));
        out.println("");
        out.println("Here is an example command (in case your values are not filled in):");
        out.println(getCommandWithDefaults(args, "myuser", "mypassword", "myhost.myplace.com", "12345", "myschema", "H:\\sybase-ddl-output.txt"));

        return false;
    }

    private String getCommandWithDefaults(AquaRevengArgs args, String username, String password, String dbHost, String dbPort, String dbSchema, String outputDirectory) {
        return "    C:\\Sybase_15_5_x64\\ASEP\\bin\\ddlgen " +
                "-U " + ObjectUtils.defaultIfNull(args.getUsername(), username) + " " +
                "-P " + ObjectUtils.defaultIfNull(args.getPassword(), password) + " " +
                "-S " + ObjectUtils.defaultIfNull(args.getDbHost(), dbHost) + ":" + ObjectUtils.defaultIfNull(args.getDbPort(), dbPort) + " " +
                "-D " + ObjectUtils.defaultIfNull(args.getDbSchema(), dbSchema) + " " +
                "-O " + ObjectUtils.defaultIfNull(args.getOutputDir(), outputDirectory);
    }
}
