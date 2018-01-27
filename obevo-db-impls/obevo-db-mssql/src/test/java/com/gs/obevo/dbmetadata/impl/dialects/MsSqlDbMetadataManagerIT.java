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
import com.gs.obevo.db.impl.platforms.mssql.MsSqlDbPlatform;
import com.gs.obevo.db.impl.platforms.mssql.MsSqlParamReader;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import org.apache.commons.dbutils.QueryRunner;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MsSqlDbMetadataManagerIT extends AbstractDbMetadataManagerIT {
    @Parameterized.Parameters
    public static Collection<Object[]> params() {
        return MsSqlParamReader.getParamReader().getJdbcDsAndSchemaParams();
    }

    public MsSqlDbMetadataManagerIT(DataSource dataSource, PhysicalSchema physicalSchema) {
        super(dataSource, physicalSchema);
    }

    @Override
    protected DbMetadataManager createMetadataManager() {
        return new MsSqlDbPlatform().getDbMetadataManager();
    }

    @Override
    protected void setCurrentSchema(QueryRunner jdbc) throws Exception {
        jdbc.update("USE " + getSchemaName());
    }

    @Override
    protected String getDropSqlFile() {
        return "mssql-test-drops.sql";
    }

    @Override
    protected String getAddSqlFile() {
        return "mssql-test.sql";
    }

    @Override
    protected boolean isPmdKnownBroken() {
        return true;
    }

    @Override
    protected String convertName(String name) {
        return name;
    }

    @Override
    protected String get_VIEW1() {
        return "CREATE VIEW " + getSubschemaString() + "VIEW1 AS SELECT * FROM " + getSubschemaString() + "METADATA_TEST_TABLE -- my comment";
    }

    @Override
    protected String get_INVALID_VIEW() {
        return "create view " + getSubschemaString() + "INVALID_VIEW AS SELECT * FROM " + getSubschemaString() + "INVALID_TABLE";
    }

    @Override
    protected String get_SP1() {
        return "CREATE PROCEDURE " + getSubschemaString() + "SP1 AS -- ensure that SP comment remains DELETE FROM " + getSubschemaString() + "TABLE_A DELETE FROM " + getSubschemaString() + "TABLE_A";
    }

    @Override
    protected String get_FUNC1() {
        return "CREATE FUNCTION " + getSubschemaString() + "FUNC1() RETURNS INT AS BEGIN -- ensure that func comment remains RETURN 10 END";
    }

    @Override
    protected String get_FUNC_WITH_OVERLOAD_3() {
        return "-- NOTE - no function overloads supported in ASE CREATE FUNCTION " + getSubschemaString() + "FUNC_WITH_OVERLOAD (@var1 INT, @INVALSTR VARCHAR(32)) RETURNS INT AS BEGIN RETURN 10 END";
    }

    @Override
    protected String get_SP_WITH_OVERLOAD_1() {
        return "CREATE PROCEDURE " + getSubschemaString() + "SP_WITH_OVERLOAD AS DELETE FROM " + getSubschemaString() + "TABLE_A";
    }

    @Override
    protected String get_SP_WITH_OVERLOAD_2() {
        return "CREATE PROCEDURE " + getSubschemaString() + "SP_WITH_OVERLOAD;2 (@INVAL INT) AS DELETE FROM " + getSubschemaString() + "TABLE_A DELETE FROM " + getSubschemaString() + "TABLE_A";
    }

    @Override
    protected String get_SP_WITH_OVERLOAD_3() {
        return "CREATE PROCEDURE " + getSubschemaString() + "SP_WITH_OVERLOAD AS DELETE FROM TABLE_A DELETE FROM " + getSubschemaString() + "TABLE_A DELETE FROM " + getSubschemaString() + "TABLE_A DELETE FROM " + getSubschemaString() + "TABLE_A";
    }

    @Override
    protected boolean isFuncOverloadSupported() {
        return false;
    }

    @Override
    protected boolean isSequenceSupported() {
        return false;
    }

    @Override
    protected boolean isRuleBindingSupported() {
        // rule binding is only supported for the default schema
        return getPhysicalSchema().getSubschema() == null;
    }

    @Override
    protected boolean isRuleSupported() {
        return true;
    }

    @Override
    protected boolean isUserTypeSupported() {
        return true;
    }

    @Override
    protected OverLoadSupport isSpOverloadSupported() {
        return OverLoadSupport.COMBINED_OBJECT;
    }
}
