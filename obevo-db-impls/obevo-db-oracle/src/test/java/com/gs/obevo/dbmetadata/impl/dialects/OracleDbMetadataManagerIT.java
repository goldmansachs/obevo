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
package com.gs.obevo.dbmetadata.impl.dialects;

import java.util.Collection;

import javax.sql.DataSource;

import com.gs.obevo.db.impl.platforms.oracle.OracleDbPlatform;
import com.gs.obevo.db.impl.platforms.oracle.OracleParamReader;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import org.apache.commons.dbutils.QueryRunner;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@Ignore("Ignoring until we open source due to lack of permissions internally; also that we cannot drop package bodies yet in the OracleDeployIT until we support them first-class")
public class OracleDbMetadataManagerIT extends AbstractDbMetadataManagerIT {
    @Parameterized.Parameters
    public static Collection<Object[]> params() {
        return OracleParamReader.getParamReader().getJdbcDsAndSchemaParams();
    }

    public OracleDbMetadataManagerIT(DataSource dataSource, String schemaName) {
        super(dataSource, schemaName);
    }

    protected DbMetadataManager createMetadataManager() {
        return new OracleDbPlatform().getDbMetadataManager();
    }

    protected void setCurrentSchema(QueryRunner jdbc) throws Exception {
        jdbc.update("alter session set current_schema = " + getSchemaName());
    }

    @Override
    protected String getDropSqlFile() {
        return "oracle-test-drops.sql";
    }

    @Override
    protected String getAddSqlFile() {
        return "oracle-test.sql";
    }

    @Override
    protected String convertName(String name) {
        return name.toUpperCase();
    }

    @Override
    protected boolean isViewSupported() {
        return false;  // not supporting for now until proper environment is obtained
    }

    @Override
    protected boolean isStoredProcedureSupported() {
        return false;  // not supporting for now; will get initial deployment out for teams
    }

    @Override
    protected boolean isFunctionSupported() {
        return false;  // not supporting for now; will get initial deployment out for teams
    }

    @Override
    protected boolean isSequenceSupported() {
        return false;  // not supporting for now; will get initial deployment out for teams
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
        // TODO should eventually have the CREATE VIEW statement prepended
        return "SELECT \"AFIELD\",\"BFIELD\" FROM METADATA_TEST_TABLE -- my comment";
    }

    @Override
    protected String get_INVALID_VIEW() {
        // TODO should eventually have the CREATE VIEW statement prepended
        return "SELECT \"A\" FROM INVALID_TABLE";
    }
}
