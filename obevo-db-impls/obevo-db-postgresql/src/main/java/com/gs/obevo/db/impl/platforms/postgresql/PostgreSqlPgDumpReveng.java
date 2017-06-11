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
package com.gs.obevo.db.impl.platforms.postgresql;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.apps.reveng.ChangeEntry;
import com.gs.obevo.db.impl.core.util.MultiLineStringSplitter;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;

public class PostgreSqlPgDumpReveng extends AbstractDdlReveng {
    public PostgreSqlPgDumpReveng() {
        super(
                new PostgreSqlDbPlatform(),
                new MultiLineStringSplitter("GO", true),
                Lists.immutable.<Predicate<String>>of(
                        StringPredicates.contains("-- PostgreSQL database dump").and(StringPredicates.contains("-- Dumped by pg_dump"))
                ),
                getRevengPatterns(),
                new Procedure2<ChangeEntry, String>() {
                    @Override
                    public void value(ChangeEntry changeEntry, String s) {

                    }
                }
        );
        setSkipLinePredicates(Lists.immutable.<Predicate<String>>of(
                StringPredicates.startsWith("SET statement_timeout")
                , StringPredicates.startsWith("SET default_tablespace")
                , StringPredicates.startsWith("SET lock_timeout")
                , StringPredicates.startsWith("SET idle_in_transaction_session_timeout")
                , StringPredicates.startsWith("SET client_encoding")
                , StringPredicates.startsWith("SET standard_conforming_strings")
                , StringPredicates.startsWith("SET check_function_bodies")
                , StringPredicates.startsWith("SET client_min_messages")
                , StringPredicates.startsWith("SET row_security")
                , StringPredicates.startsWith("SET default_with_oids")
                , StringPredicates.startsWith("CREATE SCHEMA")
                , StringPredicates.startsWith("SET search_path")
        ));
    }

    @Override
    protected void printInstructions(AquaRevengArgs args) {
        System.out.println("1) Login to your DB2 command line environment by running the following command (assuming you have the DB2 command line client installed):");
        System.out.println("    db2cmd");
        System.out.println("");
        System.out.println("That should result in a new command line window in Windows.");
        System.out.println("");
        System.out.println("");
        System.out.println("2) Run the following command to generate the DDL file:");
        System.out.println(getCommandWithDefaults(args, "<username>", "<password>", "<dbServerName>", "<dbSchema>", "<outputFile>"));
        System.out.println("");
        System.out.println("Here is an example command (in case your values are not filled in):");
        System.out.println(getCommandWithDefaults(args, "myuser", "mypassword", "MYDB2DEV01", "myschema", "H:\\db2-ddl-output.txt"));
        System.out.println("");
        System.out.println("*** Exception handling *** ");
        System.out.println("If you get an exception that you do not have the BIND privilege, e.g. 'SQL0552N  \"yourId\" does not have the privilege to perform operation \"BIND\".  SQLSTATE=42502");
        System.out.println("then run the BIND command");
        System.out.println("");
        System.out.println("");
        System.out.println("3) Once that is done, rerun the reverse-engineering command you just ran, but add the following argument based on the <outputDirectory> value passed in above the argument:");
        System.out.println("    -inputDir " + ObjectUtils.defaultIfNull(args.getOutputDir(), "<outputFile>"));

    }

    private String getCommandWithDefaults(AquaRevengArgs args, String username, String password, String dbServer, String dbSchema, String outputFile) {
        return "    db2look " +
                "-d " + ObjectUtils.defaultIfNull(args.getDbServer(), dbServer) + " " +
                "-z " + ObjectUtils.defaultIfNull(args.getDbSchema(), dbSchema) + " " +
                "-i " + ObjectUtils.defaultIfNull(args.getUsername(), username) + " " +
                "-w " + ObjectUtils.defaultIfNull(args.getPassword(), password) + " " +
                "-o " + ObjectUtils.defaultIfNull(args.getOutputDir(), outputFile) + " " +
                "-cor -e -td ~ ";
    }

    static ImmutableList<RevengPattern> getRevengPatterns() {
        String schemaNameSubPattern = getObjectPattern("", "");

        NamePatternType namePatternType = NamePatternType.ONE;
        return Lists.immutable.with(
                new RevengPattern(ChangeType.SEQUENCE_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?sequence\\s+" + schemaNameSubPattern).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+table\\s+" + schemaNameSubPattern).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
//                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)alter\\s+table\\s+" + schemaNameSubPattern + "\\s+add\\s+constraint\\s+" + nameSubPattern + "\\s+foreign\\s+key", 2, 3, "FK").withPostProcessSql(REMOVE_QUOTES),
//                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)alter\\s+table\\s+" + schemaNameSubPattern + "\\s+add\\s+constraint\\s+" + nameSubPattern, 2, 3, null).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+(?:only\\s+)" + schemaNameSubPattern).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+sequence\\s+" + schemaNameSubPattern + "\\s+owned\\s+by\\s+" + schemaNameSubPattern, 2, 1, null).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+(?:unique\\s+)index\\s+" + schemaNameSubPattern + "\\s+on\\s+" + schemaNameSubPattern, 2, 1, "INDEX").withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.FUNCTION_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?function\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.VIEW_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?view\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.SP_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)procedure\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.PACKAGE_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)package\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.TRIGGER_STR, namePatternType, "(?i)create\\s+or\\s+replace\\s+trigger\\s+" + schemaNameSubPattern)
        );
    }
}
