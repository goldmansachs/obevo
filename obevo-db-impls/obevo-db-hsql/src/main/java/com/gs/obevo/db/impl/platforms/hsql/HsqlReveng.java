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
package com.gs.obevo.db.impl.platforms.hsql;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.util.inputreader.Credential;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HsqlReveng extends AbstractDdlReveng {
    private static final Logger LOG = LoggerFactory.getLogger(HsqlReveng.class);
    private static final String QUOTE = "";

    public HsqlReveng() {
        // Note - HSQL Export currently
        super(
                new HsqlDbPlatform(),
                null,
                Lists.immutable.<Predicate<String>>of(
                        StringPredicates.startsWith("SET DATABASE")
                        , StringPredicates.startsWith("SET FILES")
                        , StringPredicates.startsWith("SET SCHEMA")
                        , StringPredicates.startsWith("INSERT INTO")
                        , StringPredicates.startsWith("CREATE USER")
                        , StringPredicates.startsWith("ALTER USER")
                        , StringPredicates.startsWith("CREATE ROLE")
                        , StringPredicates.startsWith("CREATE SCHEMA")
                        , StringPredicates.startsWith("GRANT ")
                        , StringPredicates.startsWith("ALTER SEQUENCE SYSTEM_LOBS")
                ),
                getRevengPatterns(),
                null
        );
        setStartQuote(QUOTE);
        setEndQuote(QUOTE);
    }

    private static ImmutableList<RevengPattern> getRevengPatterns() {
        String schemaNameSubPattern = getSchemaObjectPattern(QUOTE, QUOTE);
        String schemaSysNamePattern = getSchemaObjectWithPrefixPattern(QUOTE, QUOTE, "SYS_");
        NamePatternType namePatternType = NamePatternType.TWO;
        return Lists.immutable.with(
                new RevengPattern(ChangeType.SEQUENCE_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?sequence\\s+" + schemaNameSubPattern).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+(?:memory\\s+)table\\s+" + schemaNameSubPattern).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+" + schemaNameSubPattern).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+unique\\s+index\\s+" + schemaSysNamePattern + "\\s+on\\s+" + schemaNameSubPattern, 2, 1, "excludeEnvs=\"%\" comment=\"this_is_potentially_a_redundant_primaryKey_index_please_double_check\"").withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+(?:unique\\s+)index\\s+" + schemaNameSubPattern + "\\s+on\\s+" + schemaNameSubPattern, 2, 1, "INDEX").withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new RevengPattern(ChangeType.FUNCTION_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?function\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.VIEW_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?view\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.SP_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?procedure\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.USERTYPE_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?type\\s+" + schemaNameSubPattern),
                new RevengPattern(ChangeType.TRIGGER_STR, namePatternType, "(?i)create\\s+or\\s+replace\\s+trigger\\s+" + schemaNameSubPattern)
        );
        //CREATE TYPE SCHEMA1.BOOLEAN2 AS SMALLINT
    }

    @Override
    protected File printInstructions(PrintStream out, AquaRevengArgs args) {
        DbEnvironment env = getDbEnvironment(args);

        JdbcDataSourceFactory jdbcFactory = new HsqlJdbcDataSourceFactory();
        DataSource ds = jdbcFactory.createDataSource(env, new Credential(args.getUsername(), args.getPassword()), 1);
        JdbcHelper jdbc = new JdbcHelper(null, false);

        Path interim = new File(args.getOutputPath(), "interim").toPath();
        interim.toFile().mkdirs();
        try (Connection conn = ds.getConnection()) {
            // https://docs.oracle.com/database/121/ARPLS/d_metada.htm#BGBJBFGE
            // Note - can't remap schema name, object name, tablespace name within JDBC calls; we will leave that to the existing code in AbstractDdlReveng
            File outputFile = interim.resolve("output.sql").toFile();
            outputFile.delete();  // clean before creating
            jdbc.update(conn, "SCRIPT '" + outputFile.getCanonicalPath() + "'");
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }

        return interim.toFile();
    }
}
