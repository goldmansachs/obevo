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
package com.gs.obevo.dbmetadata.impl.dialects;

import java.util.Collection;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.impl.platforms.db2.Db2DbPlatform;
import com.gs.obevo.db.impl.platforms.db2.Db2ParamReader;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DaView;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import org.apache.commons.dbutils.QueryRunner;
import org.eclipse.collections.api.multimap.Multimap;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class Db2DbMetadataManagerIT extends AbstractDbMetadataManagerIT {
    @Parameterized.Parameters
    public static Collection<Object[]> params() {
        return Db2ParamReader.getParamReader().getJdbcDsAndSchemaParams();
    }

    public Db2DbMetadataManagerIT(DataSource dataSource, PhysicalSchema physicalSchema) {
        super(dataSource, physicalSchema);
    }

    protected DbMetadataManager createMetadataManager() {
        return new Db2DbPlatform().getDbMetadataManager();
    }

    protected void setCurrentSchema(QueryRunner jdbc) throws Exception {
        jdbc.update("SET CURRENT PATH " + "SYSIBM,SYSFUN,SYSPROC,SYSIBMADM,DBDEPLOY03");
        jdbc.update("SET SCHEMA " + getSchemaName());
    }

    @Override
    protected String getDropSqlFile() {
        return "db2-test-drops.sql";
    }

    @Override
    protected String getAddSqlFile() {
        return "db2-test.sql";
    }

    @Override
    protected String convertName(String name) {
        return name.toUpperCase();
    }

    @Override
    protected void verify_VIEW1(Multimap<String, DaTable> tablesByName, String tableName) {
        super.verify_VIEW1(tablesByName, tableName);
        DaView view = (DaView) tablesByName.get(tableName).toList().get(0);
        assertEquals("Y", view.getAttribute("VALID"));
    }

    @Override
    protected String get_SP_WITH_OVERLOAD_1() {
        return "CREATE PROCEDURE SP_WITH_OVERLOAD () LANGUAGE SQL DYNAMIC RESULT SETS 1 BEGIN ATOMIC DELETE FROM TABLE_A; END";
    }

    @Override
    protected String get_SP_WITH_OVERLOAD_2() {
        return "CREATE PROCEDURE SP_WITH_OVERLOAD (IN INVAL INT) LANGUAGE SQL DYNAMIC RESULT SETS 1 BEGIN ATOMIC DELETE FROM TABLE_A; END";
    }

    @Override
    protected String get_SP_WITH_OVERLOAD_3() {
        return "CREATE PROCEDURE SP_WITH_OVERLOAD (IN INVAL INT, IN INVALSTR VARCHAR(32)) LANGUAGE SQL DYNAMIC RESULT SETS 1 BEGIN ATOMIC DELETE FROM TABLE_A; END";
    }

    @Override
    protected String get_FUNC_WITH_OVERLOAD_1() {
        return "CREATE FUNCTION FUNC_WITH_OVERLOAD () RETURNS integer language SQL NOT deterministic NO EXTERNAL ACTION READS SQL DATA RETURN VALUES (1)";
    }

    @Override
    protected String get_FUNC_WITH_OVERLOAD_2() {
        return "CREATE FUNCTION FUNC_WITH_OVERLOAD (var1 integer) RETURNS integer language SQL NOT deterministic NO EXTERNAL ACTION READS SQL DATA RETURN VALUES (1)";
    }

    @Override
    protected String get_FUNC_WITH_OVERLOAD_3() {
        return "CREATE FUNCTION FUNC_WITH_OVERLOAD (var1 integer, IN INVALSTR VARCHAR(32)) RETURNS integer language SQL NOT deterministic NO EXTERNAL ACTION READS SQL DATA RETURN VALUES (1)";
    }

    @Override
    protected String get_SP1() {
        return "CREATE PROCEDURE SP1 () LANGUAGE SQL DYNAMIC RESULT SETS 1 BEGIN ATOMIC -- ensure that SP comment remains DELETE FROM TABLE_A; END";
    }

    @Override
    protected String get_FUNC1() {
        return "CREATE FUNCTION FUNC1 () RETURNS integer language SQL NOT deterministic NO EXTERNAL ACTION READS SQL DATA -- ensure that func comment remains RETURN VALUES (1)";
    }

    @Override
    protected String get_VIEW1() {
        return "CREATE VIEW VIEW1 AS SELECT * FROM METADATA_TEST_TABLE -- my comment WITH LOCAL CHECK OPTION";
    }

    @Override
    protected String get_INVALID_VIEW() {
        return "create view INVALID_VIEW AS SELECT * FROM INVALID_TABLE";
    }
}
