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
package com.gs.obevo.db.impl.platforms.db2;

import java.io.File;
import java.io.PrintStream;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.apps.reveng.RevengPattern;
import com.gs.obevo.db.apps.reveng.RevengPattern.NamePatternType;
import com.gs.obevo.impl.util.MultiLineStringSplitter;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;

public class Db2lookReveng extends AbstractDdlReveng {
    Db2lookReveng() {
        super(
                new Db2DbPlatform(),
                new MultiLineStringSplitter("~", false),
                Lists.immutable.<Predicate<String>>of(
                        StringPredicates.contains("CLP file was created using DB2LOOK")
                        , StringPredicates.startsWith("CREATE SCHEMA")
                        , StringPredicates.startsWith("SET CURRENT SCHEMA")
                        , StringPredicates.startsWith("SET CURRENT PATH")
                        , StringPredicates.startsWith("COMMIT WORK")
                        , StringPredicates.startsWith("CONNECT RESET")
                        , StringPredicates.startsWith("TERMINATE")
                        , StringPredicates.startsWith("SET NLS_STRING_UNITS = 'SYSTEM'")
                ),
                getRevengPatterns(),
                null
        );
        setStartQuote("\"");
        setEndQuote("\"");
    }

    static ImmutableList<RevengPattern> getRevengPatterns() {
        String schemaNameSubPattern = getSchemaObjectPattern("\"", "\"");
        NamePatternType namePatternType = RevengPattern.NamePatternType.TWO;
        return Lists.immutable.with(
                new RevengPattern(ChangeType.SEQUENCE_STR, namePatternType, "(?i)create\\s+or\\s+replace\\s+sequence\\s+" + schemaNameSubPattern).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+table\\s+" + schemaNameSubPattern).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + schemaNameSubPattern + "\\s+add\\s+constraint\\s+" + schemaNameSubPattern + "\\s+foreign\\s+key", 1, 2, "FK").withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + schemaNameSubPattern + "\\s+add\\s+constraint\\s+" + schemaNameSubPattern, 1, 2, null).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + schemaNameSubPattern).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+index\\s+" + schemaNameSubPattern + "\\s+on\\s+" + schemaNameSubPattern, 2, 1, "INDEX").withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.FUNCTION_STR, namePatternType, "(?i)create\\s+or\\s+replace\\s+function\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.VIEW_STR, namePatternType, "(?i)create\\s+or\\s+replace\\s+view\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.SP_STR, namePatternType, "(?i)create\\s+or\\s+replace\\s+procedure\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.TRIGGER_STR, namePatternType, "(?i)create\\s+or\\s+replace\\s+trigger\\s+" + schemaNameSubPattern)
        );
    }

    @Override
    protected boolean doRevengOrInstructions(PrintStream out, AquaRevengArgs args, File interimDir) {
        out.println("1) Login to your DB2 command line environment by running the following command (assuming you have the DB2 command line client installed):");
        out.println("    db2cmd");
        out.println("");
        out.println("That should result in a new command line window in Windows.");
        out.println("");
        out.println("");
        out.println("2) Run the following command to generate the DDL file:");
        out.println(getCommandWithDefaults(args, "<username>", "<password>", "<dbServerName>", "<dbSchema>", "<outputFile>"));
        out.println("");
        out.println("Here is an example command (in case your input arguments are not filled in):");
        out.println(getCommandWithDefaults(args, "myuser", "mypassword", "MYDB2DEV01", "myschema", "H:\\db2-ddl-output.txt"));
        out.println("");
        out.println("*** Exception handling *** ");
        out.println("If you get an exception that you do not have the BIND privilege, e.g. 'SQL0552N  \"yourId\" does not have the privilege to perform operation \"BIND\".  SQLSTATE=42502");
        out.println("then run the BIND command");

        return false;
    }

    private String getCommandWithDefaults(AquaRevengArgs args, String username, String password, String dbServer, String dbSchema, String outputFile) {
        return "    db2look " +
                "-d " + ObjectUtils.defaultIfNull(args.getDbServer(), dbServer) + " " +
                "-z " + ObjectUtils.defaultIfNull(args.getDbSchema(), dbSchema) + " " +
                "-i " + ObjectUtils.defaultIfNull(args.getUsername(), username) + " " +
                "-w " + ObjectUtils.defaultIfNull(args.getPassword(), password) + " " +
                "-o " + ObjectUtils.defaultIfNull(args.getOutputPath(), outputFile) + " " +
                "-cor -e -td ~ ";
    }
}
