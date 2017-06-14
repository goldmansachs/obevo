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
package com.gs.obevo.db.impl.platforms.mssql;

import java.io.File;
import java.io.PrintStream;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.apps.reveng.ChangeEntry;
import com.gs.obevo.db.impl.core.reader.TextMarkupDocumentReader;
import com.gs.obevo.db.impl.core.util.MultiLineStringSplitter;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;

public class MsSqlReveng extends AbstractDdlReveng {
    public MsSqlReveng() {
        super(
                new MsSqlDbPlatform(),
                new MultiLineStringSplitter("GO", true),
                Lists.immutable.<Predicate<String>>of(
                        StringPredicates.contains("-- PostgreSQL database dump").and(StringPredicates.contains("-- Dumped by pg_dump"))
                ),
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
        setStartQuote("\\[");
        setEndQuote("\\]");
        setSkipLinePredicates(Lists.immutable.<Predicate<String>>of(
                StringPredicates.matches(".*\\s*/\\*+\\s+Object")
                , StringPredicates.startsWith("/****** Object:")
                , StringPredicates.startsWith("SET ANSI_NULLS")
                , StringPredicates.startsWith("SET QUOTED_IDENTIFIER")
        ));
    }

    static ImmutableList<RevengPattern> getRevengPatterns() {
        String schemaNameSubPattern = getSchemaObjectPattern("\\[", "\\]");
        NamePatternType namePatternType = NamePatternType.TWO;

        return Lists.immutable.with(
                new AbstractDdlReveng.RevengPattern(ChangeType.USERTYPE_STR, namePatternType, "(?i)create\\s+type\\s+" + schemaNameSubPattern + "\\s+"),
                new AbstractDdlReveng.RevengPattern(ChangeType.DEFAULT_STR, namePatternType, "(?i)create\\s+default\\s+" + schemaNameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.SEQUENCE_STR, namePatternType, "(?i)create\\s+seq(uence)?\\s+" + schemaNameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+table\\s+" + schemaNameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + schemaNameSubPattern + "\\s+add\\s+constraint\\s+" + schemaNameSubPattern + "\\s+foreign\\s+key", 1, 2, "FK"),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + schemaNameSubPattern + "\\s+add\\s+constraint\\s+" + schemaNameSubPattern, 1, 2, null),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + schemaNameSubPattern + "\\s+"),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+(?:(?:unique)|(?:nonclustered)\\s+)?(?:\\w+\\s+)?index\\s+" + schemaNameSubPattern + "\\s+on\\s+" + schemaNameSubPattern, 2, 1, "INDEX"),
                new AbstractDdlReveng.RevengPattern(ChangeType.FUNCTION_STR, namePatternType, "(?i)create\\s+func(tion)?\\s+" + schemaNameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.VIEW_STR, namePatternType, "(?i)create\\s+view\\s+" + schemaNameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.SP_STR, namePatternType, "(?i)create\\s+proc(?:edure)?\\s+" + schemaNameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.TRIGGER_STR, namePatternType, "(?i)create\\s+trigger\\s+" + schemaNameSubPattern + "on\\s+" + schemaNameSubPattern, 2, 1, null),
                new AbstractDdlReveng.RevengPattern(ChangeType.RULE_STR, namePatternType, "(?i)create\\s+rule\\s+" + schemaNameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.USERTYPE_STR, namePatternType, "(?i)^(exec\\s+)?sp_addtype\\s+'(\\w+)'")
        );
    }

    @Override
    protected File printInstructions(PrintStream out, AquaRevengArgs args) {
        out.println("1) Download the powershell script from: https://github.com/goldmansachs/obevo/tree/master/obevo-db-impls/obevo-db-mssql/src/main/resources/SqlServerDdlReveng.ps1");
        out.println("");
        out.println("2) Open a powershell prompt (assuming you have one installed):");
        out.println("");
        out.println("3) Source the script, e.g.:");
        out.println("");
        out.println("    . .\\SqlServerDdlReveng.ps1");
        out.println("");
        out.println("4) Run the following command to generate the DDL file:");
        out.println(getCommandWithDefaults(args, "<username>", "<password>", "<dbHost>", "<database>", "<outputFile>"));
        out.println("");
        out.println("Here is an example command (in case your input arguments are not filled in):");
        out.println(getCommandWithDefaults(args, "myuser", "mypassword", "myhost.me.com", "mydatabase", "H:\\db2-ddl-output.txt"));
        out.println("");
        out.println("*******");
        out.println("NOTE - This script is still in beta and subject to signature changes.");
        out.println("Please give it a try and provide us feedback, or contribute changes as needed.");

        return null;
    }


    private String getCommandWithDefaults(AquaRevengArgs args, String username, String password, String dbHost, String dbSchema, String outputFile) {
        return "    SqlServerDdlReveng " +
                " " + ObjectUtils.defaultIfNull(args.getOutputPath(), outputFile) +
                " " + ObjectUtils.defaultIfNull(args.getDbHost(), dbHost) +
                " " + ObjectUtils.defaultIfNull(args.getDbSchema(), dbSchema) +
                " " + ObjectUtils.defaultIfNull(args.getUsername(), username) +
                " " + ObjectUtils.defaultIfNull(args.getPassword(), password);
    }
}
