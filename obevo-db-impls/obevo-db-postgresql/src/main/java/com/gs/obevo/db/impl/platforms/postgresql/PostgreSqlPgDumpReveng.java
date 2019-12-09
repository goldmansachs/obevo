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
package com.gs.obevo.db.impl.platforms.postgresql;

import java.io.File;
import java.io.PrintStream;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.apps.reveng.AquaRevengArgs;
import com.gs.obevo.apps.reveng.RevengPattern;
import com.gs.obevo.apps.reveng.RevengPattern.NamePatternType;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.impl.util.MultiLineStringSplitter;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;

public class PostgreSqlPgDumpReveng extends AbstractDdlReveng {
    PostgreSqlPgDumpReveng() {
        super(
                new PostgreSqlDbPlatform(),
                new MultiLineStringSplitter("GO", true),  // the GO comes from a hack in AbstractReveng - should ideally be fixed
                Lists.immutable.<Predicate<String>>of(
                        StringPredicates.contains("-- PostgreSQL database dump").and(StringPredicates.contains("-- Dumped by pg_dump"))
                ),
                getRevengPatterns(),
                null
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

    private static ImmutableList<RevengPattern> getRevengPatterns() {
        String schemaNameSubPattern = getSchemaObjectPattern("", "");
        String objectNameSubPattern = getObjectPattern("", "");

        NamePatternType namePatternType = RevengPattern.NamePatternType.TWO;
        return Lists.immutable.with(
                new RevengPattern(ChangeType.SEQUENCE_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?sequence\\s+" + schemaNameSubPattern).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+table\\s+" + schemaNameSubPattern).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+(?:only\\s+)" + schemaNameSubPattern).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+sequence\\s+" + schemaNameSubPattern + "\\s+owned\\s+by\\s+" + schemaNameSubPattern, 2, 1, null).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+(?:unique\\s+)index\\s+" + schemaNameSubPattern + "\\s+on\\s+" + schemaNameSubPattern, 2, 1, "INDEX").withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.FUNCTION_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?function\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.SP_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?procedure\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.VIEW_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?view\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.SP_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)procedure\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.PACKAGE_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)package\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.TRIGGER_STR, namePatternType, "(?i)create\\s+or\\s+replace\\s+trigger\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.USERTYPE_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?type\\s+" + schemaNameSubPattern)
        );
    }

    @Override
    protected boolean doRevengOrInstructions(PrintStream out, AquaRevengArgs args, File interimDir) {
        out.println("1) Create the folder for your interim output:");
        out.println();
        out.println("mkdir -p " + interimDir.getAbsolutePath());
        out.println();
        out.println();
        out.println("2) Run the following command to generate the DDL file:");
        out.println();
        out.println("(without Docker)");
        out.println(getCommandWithDefaults(false, args, interimDir, "<username>", "<password>", "<dbHost>", "<dbPortNumber>", "<dbName>", "<dbSchema>", "<outputFile>"));
        out.println();
        out.println("(with Docker)");
        out.println(getCommandWithDefaults(true, args, interimDir, "<username>", "<password>", "<dbHost>", "<dbPortNumber>", "<dbName>", "<dbSchema>", "<outputFile>"));
        out.println();
        out.println("Here is an example command (in case your values are not filled in):");
        out.println();
        out.println("(without Docker)");
        out.println(getCommandWithDefaults(false, args, interimDir, "myuser", "mypassword", "myhost.myplace.com", "12345", "mydb", "myschema", "H:\\sybase-ddl-output.txt"));
        out.println();
        out.println("(with Docker)");
        out.println(getCommandWithDefaults(true, args, interimDir, "myuser", "mypassword", "myhost.myplace.com", "12345", "mydb", "myschema", "H:\\sybase-ddl-output.txt"));
        out.println();

        return false;
    }

    private String getCommandWithDefaults(boolean container, AquaRevengArgs args, File interimDir, String username, String password, String dbHost, String dbPort, String dbName, String dbSchema, String outputDirectory) {
        String prefix = container ? "docker exec $CONTAINER_NAME " : "";
        String fileSuffix = container ? "> " : " -f ";
        fileSuffix += new File(interimDir, "revengoutput.txt").getAbsolutePath();
        return prefix + "pg_dump -O -s --no-privileges" +
                " -h " + ObjectUtils.defaultIfNull(args.getDbHost(), dbHost) +
                " -p " + ObjectUtils.defaultIfNull(args.getDbPort(), dbPort) +
                " --username=" + ObjectUtils.defaultIfNull(args.getUsername(), username) +
                " -d " + ObjectUtils.defaultIfNull(args.getDbServer(), dbName) +
                " -n " + ObjectUtils.defaultIfNull(args.getDbSchema(), dbSchema) +
                " " + fileSuffix;
    }
}
