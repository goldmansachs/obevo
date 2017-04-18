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
package com.gs.obevo.db.impl.platforms.db2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.apps.reveng.ChangeEntry;
import com.gs.obevo.db.impl.core.util.MultiLineStringSplitter;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;

public class Db2lookReveng extends AbstractDdlReveng {
    public Db2lookReveng() {
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
                new Procedure2<ChangeEntry, String>() {
                    @Override
                    public void value(ChangeEntry changeEntry, String s) {

                    }
                }
        );
    }

    @Override
    protected void printInstructions(AquaRevengArgs args) {
        System.out.println("1) Login to your DB2 command line environment by running the following command:");
        System.out.println("    db2cmd");
        System.out.println("");
        System.out.println("That should result in a new command line window in Windows.");
        System.out.println("");
        System.out.println("*** If that path is not found, then install the 'DB2 Client' from GS AppStore*** ");
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

    static final Function<String, LineParseOutput> REMOVE_QUOTES = new Function<String, AbstractDdlReveng.LineParseOutput>() {
        @Override
        public AbstractDdlReveng.LineParseOutput valueOf(String input) {
            return new AbstractDdlReveng.LineParseOutput(removeQuotes(input));
        }
    };
    static final Function<String, AbstractDdlReveng.LineParseOutput> REPLACE_TABLESPACE = new Function<String, AbstractDdlReveng.LineParseOutput>() {
        @Override
        public AbstractDdlReveng.LineParseOutput valueOf(String input) {
            return substituteTablespace(input);
        }
    };

    static ImmutableList<RevengPattern> getRevengPatterns() {
        String nameSubPattern = "\"?(\\w+)\"?";
        String schemaNameSubPattern = "\"?(\\w+\\.)?(\\w+)\"?";
        return Lists.immutable.with(
                new AbstractDdlReveng.RevengPattern(ChangeType.SEQUENCE_STR, "(?i)create\\s+or\\s+replace\\s+sequence\\s+" + schemaNameSubPattern, 2, null, null).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.SEQUENCE_STR, "(?i)create\\s+or\\s+replace\\s+sequence\\s+" + nameSubPattern).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)create\\s+table\\s+" + schemaNameSubPattern, 2, null, null).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)create\\s+table\\s+" + nameSubPattern).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)alter\\s+table\\s+" + schemaNameSubPattern + "\\s+add\\s+constraint\\s+" + nameSubPattern + "\\s+foreign\\s+key", 2, 3, "FK").withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)alter\\s+table\\s+" + nameSubPattern + "\\s+add\\s+constraint\\s+" + nameSubPattern + "\\s+foreign\\s+key", 1, 2, "FK").withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)alter\\s+table\\s+" + schemaNameSubPattern + "\\s+add\\s+constraint\\s+" + nameSubPattern, 2, 3, null).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)alter\\s+table\\s+" + nameSubPattern + "\\s+add\\s+constraint\\s+" + nameSubPattern, 1, 2, null).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)alter\\s+table\\s+" + schemaNameSubPattern, 2, null, null).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)alter\\s+table\\s+" + nameSubPattern).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)create\\s+index\\s+" + schemaNameSubPattern + "\\s+on\\s+" + nameSubPattern, 3, 2, "INDEX").withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)create\\s+index\\s+" + nameSubPattern + "\\s+on\\s+" + nameSubPattern, 2, 1, "INDEX").withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.FUNCTION_STR, "(?i)create\\s+or\\s+replace\\s+function\\s+" + schemaNameSubPattern, 2, null, null),
                new AbstractDdlReveng.RevengPattern(ChangeType.FUNCTION_STR, "(?i)create\\s+or\\s+replace\\s+function\\s+" + nameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.VIEW_STR, "(?i)create\\s+or\\s+replace\\s+view\\s+" + schemaNameSubPattern, 2, null, null),
                new AbstractDdlReveng.RevengPattern(ChangeType.VIEW_STR, "(?i)create\\s+or\\s+replace\\s+view\\s+" + nameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.SP_STR, "(?i)create\\s+or\\s+replace\\s+procedure\\s+" + schemaNameSubPattern, 2, null, null),
                new AbstractDdlReveng.RevengPattern(ChangeType.SP_STR, "(?i)create\\s+or\\s+replace\\s+procedure\\s+" + nameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.TRIGGER_STR, "(?i)create\\s+or\\s+replace\\s+trigger\\s+" + schemaNameSubPattern, 2, null, null),
                new AbstractDdlReveng.RevengPattern(ChangeType.TRIGGER_STR, "(?i)create\\s+or\\s+replace\\s+trigger\\s+" + nameSubPattern)
        );
    }

    public static String removeQuotes(String input) {
        Pattern compile = Pattern.compile("\"([A-Z_0-9]+)\"", Pattern.DOTALL);

        StringBuffer sb = new StringBuffer(input.length());

        Matcher matcher = compile.matcher(input);
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static AbstractDdlReveng.LineParseOutput substituteTablespace(String input) {
        Pattern compile = Pattern.compile("(\\s+IN\\s+)\"(\\w+)\"(\\s*)", Pattern.DOTALL);

        StringBuffer sb = new StringBuffer(input.length());

        String addedToken = null;
        String addedValue = null;
        Matcher matcher = compile.matcher(input);
        if (matcher.find()) {
            addedToken = matcher.group(2) + "_token";
            addedValue = matcher.group(2);
            matcher.appendReplacement(sb, matcher.group(1) + "\"\\${" + addedToken + "}\"" + matcher.group(3));
        }
        matcher.appendTail(sb);

        return new AbstractDdlReveng.LineParseOutput(sb.toString()).withToken(addedToken, addedValue);
    }
}
