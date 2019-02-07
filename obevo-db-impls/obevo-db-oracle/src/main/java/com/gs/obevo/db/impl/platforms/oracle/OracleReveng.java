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
package com.gs.obevo.db.impl.platforms.oracle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.impl.util.MultiLineStringSplitter;
import com.gs.obevo.util.inputreader.Credential;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OracleReveng extends AbstractDdlReveng {
    private static final Logger LOG = LoggerFactory.getLogger(OracleReveng.class);
    private static final String QUOTE = "\"";

    OracleReveng() {
        super(
                new OracleDbPlatform(),
                new MultiLineStringSplitter("~", true),
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
        setStartQuote(QUOTE);
        setEndQuote(QUOTE);
    }

    private static ImmutableList<RevengPattern> getRevengPatterns() {
        final String schemaNameSubPattern = getSchemaObjectPattern(QUOTE, QUOTE);
        String schemaSysNamePattern = getSchemaObjectWithPrefixPattern(QUOTE, QUOTE, "SYS_");
        NamePatternType namePatternType = NamePatternType.TWO;

        // need this function to split the package and package body lines, as the Oracle reveng function combines them together
        Function<String, LineParseOutput> prependBodyLineToPackageBody = new Function<String, LineParseOutput>() {
            private final Pattern packageBodyPattern = Pattern.compile("(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)package\\s+body\\s+" + schemaNameSubPattern, Pattern.DOTALL);

            @Override
            public LineParseOutput valueOf(String object) {
                Matcher matcher = packageBodyPattern.matcher(object);
                if (matcher.find()) {
                    String output = object.substring(0, matcher.start()) + "\n//// BODY\n" + object.substring(matcher.start());
                    return new LineParseOutput(output);
                }
                return new LineParseOutput(object);
            }
        };
        return Lists.immutable.with(
                new AbstractDdlReveng.RevengPattern(ChangeType.SEQUENCE_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?sequence\\s+" + schemaNameSubPattern).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+table\\s+" + schemaNameSubPattern).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + schemaNameSubPattern).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+unique\\s+index\\s+" + schemaSysNamePattern + "\\s+on\\s+" + schemaNameSubPattern, 2, 1, "excludeEnvs=\"%\" comment=\"this_is_potentially_a_redundant_primaryKey_index_please_double_check\"").withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+(?:unique\\s+)index\\s+" + schemaNameSubPattern + "\\s+on\\s+" + schemaNameSubPattern, 2, 1, "INDEX").withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.FUNCTION_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?function\\s+" + schemaNameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.VIEW_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?view\\s+" + schemaNameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.SP_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)procedure\\s+" + schemaNameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.PACKAGE_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)package\\s+" + schemaNameSubPattern).withPostProcessSql(prependBodyLineToPackageBody),
                new AbstractDdlReveng.RevengPattern(ChangeType.SYNONYM_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)synonym\\s+" + schemaNameSubPattern),
                new AbstractDdlReveng.RevengPattern(ChangeType.TRIGGER_STR, namePatternType, "(?i)create\\s+or\\s+replace\\s+trigger\\s+" + schemaNameSubPattern)
        );
    }

    @Override
    protected boolean doRevengOrInstructions(PrintStream out, AquaRevengArgs args, File interimDir) {
        DbEnvironment env = getDbEnvironment(args);

        JdbcDataSourceFactory jdbcFactory = new OracleJdbcDataSourceFactory();
        DataSource ds = jdbcFactory.createDataSource(env, new Credential(args.getUsername(), args.getPassword()), 1);
        JdbcHelper jdbc = new JdbcHelper(null, false);

        interimDir.mkdirs();
        try (Connection conn = ds.getConnection();
             BufferedWriter fileWriter = Files.newBufferedWriter(interimDir.toPath().resolve("output.sql"), Charset.defaultCharset())) {
            // https://docs.oracle.com/database/121/ARPLS/d_metada.htm#BGBJBFGE
            // Note - can't remap schema name, object name, tablespace name within JDBC calls; we will leave that to the existing code in AbstractDdlReveng
            jdbc.update(conn, "{ CALL DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'STORAGE',false) }");

            // we exclude:
            // PACKAGE BODY as those are generated via package anyway
            // DATABASE LINK as the get_ddl function doesn't work with it. We may support this later on
            MutableList<Map<String, Object>> maps = jdbc.queryForList(conn,
                    "SELECT CASE WHEN OBJECT_TYPE = 'TABLE' THEN 1 WHEN OBJECT_TYPE = 'INDEX' THEN 2 ELSE 3 END SORT_ORDER,\n" +
                            "    OBJECT_TYPE,\n" +
                            "    dbms_metadata.get_ddl(REPLACE(object_type,' ','_'), object_name, owner) || ';' AS object_ddl\n" +
                            "FROM DBA_OBJECTS WHERE OWNER = '" + args.getDbSchema() + "' AND OBJECT_TYPE NOT IN ('PACKAGE BODY', 'LOB', 'TABLE PARTITION', 'DATABASE LINK')\n" +
                            "      AND OBJECT_NAME NOT LIKE 'MLOG$%' AND OBJECT_NAME NOT LIKE 'RUPD$%'" +  // exclude the helper tables for materialized views
                            "      AND OBJECT_NAME NOT LIKE 'SYS_%' " +  // exclude other system tables
                            "ORDER BY 1");

            for (Map<String, Object> map : maps) {
                Clob clobObject = (Clob) map.get("OBJECT_DDL");
                String clobAsString = clobToString(clobObject);
                clobAsString = clobAsString.replaceAll(";.*$", "");

                LOG.debug("Content for {}: ", map.get("OBJECT_TYPE"), clobAsString);
                fileWriter.write(clobAsString);
                fileWriter.newLine();
                fileWriter.write("~");
                fileWriter.newLine();
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    private String clobToString(Clob clobObject) {
        try (Reader in = clobObject.getCharacterStream()) {
            StringWriter w = new StringWriter();
            IOUtils.copy(in, w);
            return w.toString();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
