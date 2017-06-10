package com.gs.obevo.db.impl.platforms.oracle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.apps.reveng.ChangeEntry;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.core.util.MultiLineStringSplitter;
import com.gs.obevo.util.inputreader.Credential;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;

public class OracleReveng extends AbstractDdlReveng {
    public OracleReveng() {
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
                new Procedure2<ChangeEntry, String>() {
                    @Override
                    public void value(ChangeEntry changeEntry, String s) {

                    }
                }
        );
    }

    @Override
    protected void printInstructions(AquaRevengArgs args) {

    }

    @Override
    protected File doNativeReveng(AquaRevengArgs args, DbEnvironment env) {
        JdbcDataSourceFactory jdbcFactory = new OracleJdbcDataSourceFactory();
        DataSource ds = jdbcFactory.createDataSource(env, new Credential(args.getUsername(), args.getPassword()), 1);
        JdbcHelper jdbc = new JdbcHelper(null, false);

        // can't remap schema name, object name, tablespace name
        String trySql = "DECLARE\n" +
                "h NUMBER; --handle returned by OPEN\n" +
                "th NUMBER; -- handle returned by ADD_TRANSFORM\n" +
                "doc CLOB;\n" +
                "my_cursor SYS_REFCURSOR;\n" +
                "BEGIN\n" +
                "\n" +
                "-- Specify the object type.\n" +
                "h := DBMS_METADATA.OPEN('TABLE');\n" +
                "\n" +
                "-- Use filters to specify the particular object desired.\n" +
                "DBMS_METADATA.SET_FILTER(h,'SCHEMA','DBDEPLOY01');\n" +
                "DBMS_METADATA.SET_FILTER(h,'NAME','TABLE_B');\n" +
                "\n" +
                " -- Request that the metadata be transformed into creation DDL.\n" +
                "th := DBMS_METADATA.ADD_TRANSFORM(h,'DDL');\n" +
                "\n" +
                " -- Fetch the object.\n" +
                "doc := DBMS_METADATA.FETCH_CLOB(h);\n" +
                "\n" +
                " -- Release resources.\n" +
                "DBMS_METADATA.CLOSE(h);\n" +
                "OPEN my_cursor FOR SELECT doc FROM DUAL;\n" +
                "--end;\n" +
                "--return my_cursor;\n" +
                "end;";
        ;
        Path interim = new File(args.getOutputPath(), "interim").toPath();
        interim.toFile().mkdirs();
        try (Connection conn = ds.getConnection();
             BufferedWriter fileWriter = Files.newBufferedWriter(interim.resolve("output.sql"), Charset.defaultCharset())) {

            // https://docs.oracle.com/database/121/ARPLS/d_metada.htm#BGBJBFGE
            jdbc.update(conn, "{ CALL DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'STORAGE',false) }");


            MutableList<Map<String, Object>> maps = jdbc.queryForList(conn,
                    "SELECT CASE WHEN OBJECT_TYPE = 'TABLE' THEN 1 WHEN OBJECT_TYPE = 'INDEX' THEN 2 ELSE 3 END SORT_ORDER,\n" +
                            "    OBJECT_TYPE,\n" +
                            "    dbms_metadata.get_ddl(REPLACE(object_type,' ','_'), object_name, owner) || ';' AS object_ddl\n" +
                            "FROM DBA_OBJECTS WHERE OWNER = '" + args.getDbSchema() + "' AND OBJECT_TYPE NOT IN ('PACKAGE BODY', 'LOB','MATERIALIZED VIEW', 'TABLE PARTITION')\n" +
                            "ORDER BY 1");

            for (Map<String, Object> map : maps) {
                System.out.println(map);
                Clob clobObject = (Clob) map.get("OBJECT_DDL");
                InputStream in = clobObject.getAsciiStream();
                StringWriter w = new StringWriter();
                try {
                    IOUtils.copy(in, w);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String clobAsString = w.toString();
                clobAsString = clobAsString.replaceAll(";.*$", "");

                System.out.println("Content for " + map.get("OBJECT_TYPE") + ": " + clobAsString);
                fileWriter.write(clobAsString);
                fileWriter.newLine();
                fileWriter.write("~");
                fileWriter.newLine();
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("DONE!!!!!!");
        return interim.toFile();
    }

    @Override
    protected boolean isNativeRevengSupported() {
        return true;
    }

    static ImmutableList<RevengPattern> getRevengPatterns() {
        String schemaNameSubPattern = getSchemaObjectPattern("\"", "\"");
        String schemaSysNamePattern = getSchemaObjectWithPrefixPattern("\"", "\"", "SYS_");
        return Lists.immutable.with(
                new AbstractDdlReveng.RevengPattern(ChangeType.SEQUENCE_STR, "(?i)create\\s+(?:or\\s+replace\\s+)?sequence\\s+" + schemaNameSubPattern, 2, null, null).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)create\\s+table\\s+" + schemaNameSubPattern, 2, null, null).withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)alter\\s+table\\s+" + schemaNameSubPattern, 2, null, null).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)create\\s+unique\\s+index\\s+" + schemaSysNamePattern + "\\s+on\\s+" + schemaNameSubPattern, 4, 2, "excludeEnvs=\"%\" comment=\"this_is_potentially_a_redundant_primaryKey_index_please_double_check\"").withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.TABLE_STR, "(?i)create\\s+(?:unique\\s+)index\\s+" + schemaNameSubPattern + "\\s+on\\s+" + schemaNameSubPattern, 4, 2, "INDEX").withPostProcessSql(REPLACE_TABLESPACE).withPostProcessSql(REMOVE_QUOTES),
                new AbstractDdlReveng.RevengPattern(ChangeType.FUNCTION_STR, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?function\\s+" + schemaNameSubPattern, 2, null, null),
                new AbstractDdlReveng.RevengPattern(ChangeType.VIEW_STR, "(?i)create\\s+(?:or\\s+replace\\s+)?(?:force\\s+)?(?:editionable\\s+)?view\\s+" + schemaNameSubPattern, 2, null, null),
                new AbstractDdlReveng.RevengPattern(ChangeType.SP_STR, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)procedure\\s+" + schemaNameSubPattern, 2, null, null),
                new AbstractDdlReveng.RevengPattern(ChangeType.PACKAGE_STR, "(?i)create\\s+(?:or\\s+replace\\s+)(?:editionable\\s+)package\\s+" + schemaNameSubPattern, 2, null, null),
                new AbstractDdlReveng.RevengPattern(ChangeType.TRIGGER_STR, "(?i)create\\s+or\\s+replace\\s+trigger\\s+" + schemaNameSubPattern, 2, null, null)
        );
    }
}
