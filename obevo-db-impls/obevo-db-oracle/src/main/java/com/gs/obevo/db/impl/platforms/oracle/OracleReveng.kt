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
package com.gs.obevo.db.impl.platforms.oracle

import com.gs.obevo.api.platform.ChangeType
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng
import com.gs.obevo.db.apps.reveng.AquaRevengArgs
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper
import com.gs.obevo.impl.util.MultiLineStringSplitter
import com.gs.obevo.util.inputreader.Credential
import org.apache.commons.io.IOUtils
import org.eclipse.collections.api.block.function.Function
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.impl.block.factory.StringPredicates
import org.eclipse.collections.impl.factory.Lists
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintStream
import java.io.StringWriter
import java.nio.charset.Charset
import java.nio.file.Files
import java.sql.Clob
import java.util.regex.Pattern

internal class OracleReveng
    : AbstractDdlReveng(
        OracleDbPlatform(),
        MultiLineStringSplitter("~", true),
        Lists.immutable.of(
                StringPredicates.contains("CLP file was created using DB2LOOK"),
                StringPredicates.startsWith("CREATE SCHEMA"),
                StringPredicates.startsWith("SET CURRENT SCHEMA"),
                StringPredicates.startsWith("SET CURRENT PATH"),
                StringPredicates.startsWith("COMMIT WORK"),
                StringPredicates.startsWith("CONNECT RESET"),
                StringPredicates.startsWith("TERMINATE"),
                StringPredicates.startsWith("SET NLS_STRING_UNITS = 'SYSTEM'")
        ),
        revengPatterns,
        null) {
    init {
        setStartQuote(QUOTE)
        setEndQuote(QUOTE)
    }

    override fun doRevengOrInstructions(out: PrintStream, args: AquaRevengArgs, interimDir: File): Boolean {
        val env = getDbEnvironment(args)

        val jdbcFactory = OracleJdbcDataSourceFactory()
        val ds = jdbcFactory.createDataSource(env, Credential(args.username, args.password), 1)
        val jdbc = JdbcHelper(null, false)

        interimDir.mkdirs()

        ds.connection.use { conn ->
            val bufferedWriter = Files.newBufferedWriter(interimDir.toPath().resolve("output.sql"), Charset.defaultCharset())
            bufferedWriter.use { fileWriter ->
                // https://docs.oracle.com/database/121/ARPLS/d_metada.htm#BGBJBFGE
                // Note - can't remap schema name, object name, tablespace name within JDBC calls; we will leave that to the existing code in AbstractDdlReveng
                jdbc.update(conn, "begin DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'STORAGE',false); end;")
                jdbc.update(conn, "begin DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'SQLTERMINATOR',true); end;")

                // we exclude:
                // PACKAGE BODY as those are generated via package anyway
                // DATABASE LINK as the get_ddl function doesn't work with it. We may support this later on
                val sql = """
select CASE WHEN obj.OBJECT_TYPE = 'TABLE' THEN 1 WHEN obj.OBJECT_TYPE = 'INDEX' THEN 3 ELSE 4 END SORT_ORDER
    , obj.OBJECT_NAME
    , obj.OBJECT_TYPE
    , dbms_metadata.get_ddl(REPLACE(obj.OBJECT_TYPE,' ','_'), obj.OBJECT_NAME, obj.owner) || ';' AS object_ddl
FROM DBA_OBJECTS obj
LEFT JOIN DBA_TABLES tab ON obj.OBJECT_TYPE = 'TABLE' AND obj.OWNER = tab.OWNER and obj.OBJECT_NAME = tab.TABLE_NAME
WHERE obj.OWNER = '${args.dbSchema}'
    AND obj.OBJECT_TYPE NOT IN ('PACKAGE BODY', 'LOB', 'TABLE PARTITION', 'DATABASE LINK')
    AND obj.OBJECT_NAME NOT LIKE 'MLOG${'$'}%' AND obj.OBJECT_NAME NOT LIKE 'RUPD${'$'}%'  -- exclude the helper tables for materialized views
    AND obj.OBJECT_NAME NOT LIKE 'SYS_%'  -- exclude other system tables
    AND (tab.NESTED is null OR tab.NESTED = 'NO')
"""

                val commentSql = """
select 2 SORT_ORDER  -- sort comments after tables but before indices
    , com.TABLE_NAME as OBJECT_NAME
    , 'COMMENT' as OBJECT_TYPE
    , dbms_metadata.get_dependent_ddl('COMMENT', com.TABLE_NAME, com.owner) || ';' AS object_ddl
FROM DBA_TAB_COMMENTS com
WHERE com.OWNER = '${args.dbSchema}'
    and TABLE_NAME not like 'BIN${'$'}%'  -- drop tables remain in the DB in the recycling bin; should ignore these
    and comments is not null  -- tables w/out colums have this value as null

    ORDER BY 1, 2
                """

                val queryResults = listOf(sql, commentSql)
                        .flatMap { jdbc.queryForList(conn, it) }
                        .sortedWith(compareBy({ it["SORT_ORDER"] as Comparable<*> }, { it["OBJECT_NAME"] as String }))
                queryResults.forEach { map ->
                    val objectType = map["OBJECT_TYPE"] as String
                    val clobObject = map["OBJECT_DDL"] as Clob
                    var clobAsString = clobToString(clobObject)

                    // TODO all parsing like this should move into the core AbstractReveng logic so that we can do more unit-test logic around this parsing
                    clobAsString = clobAsString.trimEnd()
                    clobAsString = clobAsString.replace(";+\\s*$".toRegex(RegexOption.DOT_MATCHES_ALL), "")  // remove ending semi-colons from generated SQL
                    clobAsString = clobAsString.replace("\\/+\\s*$".toRegex(RegexOption.DOT_MATCHES_ALL), "")  // some generated SQLs end in /
                    if (objectType.contains("PACKAGE")) {
                        clobAsString = clobAsString.replace("^\\/$".toRegex(RegexOption.MULTILINE), "")
                    }
                    clobAsString = clobAsString.trimEnd()

                    LOG.debug("Content for {}: {}", objectType, clobAsString)

                    val sqlsToWrite = if (objectType.equals("COMMENT")) clobAsString.split(";$".toRegex(RegexOption.MULTILINE)) else listOf(clobAsString)

                    sqlsToWrite.forEach {
                        fileWriter.write(it)
                        fileWriter.newLine()
                        fileWriter.write("~")
                        fileWriter.newLine()
                    }

                }

            }
            jdbc.update(conn, "begin DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'DEFAULT',true); end;")
        }

        return true
    }

    private fun clobToString(clobObject: Clob): String {
        clobObject.characterStream.use {
            val w = StringWriter()
            IOUtils.copy(it, w)
            return w.toString()
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OracleReveng::class.java)
        private val QUOTE = "\""

        private val revengPatterns: ImmutableList<AbstractDdlReveng.RevengPattern>
            get() {
                val schemaNameSubPattern = AbstractDdlReveng.getSchemaObjectPattern(QUOTE, QUOTE)
                val schemaSysNamePattern = AbstractDdlReveng.getSchemaObjectWithPrefixPattern(QUOTE, QUOTE, "SYS_")
                val namePatternType = AbstractDdlReveng.NamePatternType.TWO
                // need this function to split the package and package body lines, as the Oracle reveng function combines them together
                val prependBodyLineToPackageBody = object : Function<String, AbstractDdlReveng.LineParseOutput> {
                    private val packageBodyPattern = Pattern.compile("(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)package\\s+body\\s+$schemaNameSubPattern", Pattern.DOTALL)

                    override fun valueOf(sql: String): AbstractDdlReveng.LineParseOutput {
                        val matcher = packageBodyPattern.matcher(sql)
                        if (matcher.find()) {
                            val output = sql.substring(0, matcher.start()) + "\n//// BODY\n" + sql.substring(matcher.start())
                            return AbstractDdlReveng.LineParseOutput(output)
                        }
                        return AbstractDdlReveng.LineParseOutput(sql)
                    }
                }
                return Lists.immutable.with(
                        AbstractDdlReveng.RevengPattern(ChangeType.SEQUENCE_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?sequence\\s+$schemaNameSubPattern").withPostProcessSql(AbstractDdlReveng.REPLACE_TABLESPACE).withPostProcessSql(AbstractDdlReveng.REMOVE_QUOTES),
                        AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+table\\s+$schemaNameSubPattern").withPostProcessSql(AbstractDdlReveng.REPLACE_TABLESPACE).withPostProcessSql(AbstractDdlReveng.REMOVE_QUOTES),
                        AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+$schemaNameSubPattern").withPostProcessSql(AbstractDdlReveng.REMOVE_QUOTES),
                        AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)comment\\s+on\\s+(?:\\w+)\\s+$schemaNameSubPattern").withPostProcessSql(AbstractDdlReveng.REMOVE_QUOTES),
                        AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+unique\\s+index\\s+$schemaSysNamePattern\\s+on\\s+$schemaNameSubPattern", 2, 1, "excludeEnvs=\"%\" comment=\"this_is_potentially_a_redundant_primaryKey_index_please_double_check\"").withPostProcessSql(AbstractDdlReveng.REPLACE_TABLESPACE).withPostProcessSql(AbstractDdlReveng.REMOVE_QUOTES),
                        AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+(?:unique\\s+)index\\s+$schemaNameSubPattern\\s+on\\s+$schemaNameSubPattern", 2, 1, "INDEX").withPostProcessSql(AbstractDdlReveng.REPLACE_TABLESPACE).withPostProcessSql(AbstractDdlReveng.REMOVE_QUOTES),
                        AbstractDdlReveng.RevengPattern(ChangeType.FUNCTION_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?function\\s+$schemaNameSubPattern"),
                        AbstractDdlReveng.RevengPattern(ChangeType.VIEW_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?view\\s+$schemaNameSubPattern"),
                        AbstractDdlReveng.RevengPattern(ChangeType.SP_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)procedure\\s+$schemaNameSubPattern"),
                        AbstractDdlReveng.RevengPattern(ChangeType.USERTYPE_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)type\\s+$schemaNameSubPattern"),
                        AbstractDdlReveng.RevengPattern(ChangeType.PACKAGE_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)package\\s+$schemaNameSubPattern").withPostProcessSql(prependBodyLineToPackageBody),
                        AbstractDdlReveng.RevengPattern(ChangeType.SYNONYM_STR, namePatternType, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)synonym\\s+$schemaNameSubPattern"),
                        AbstractDdlReveng.RevengPattern(ChangeType.TRIGGER_STR, namePatternType, "(?i)create\\s+or\\s+replace\\s+trigger\\s+$schemaNameSubPattern")
                )
            }
    }
}
