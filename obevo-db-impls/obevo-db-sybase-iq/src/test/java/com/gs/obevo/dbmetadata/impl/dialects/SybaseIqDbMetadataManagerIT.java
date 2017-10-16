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

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.impl.platforms.sybaseiq.IqDbPlatform;
import com.gs.obevo.db.impl.platforms.sybaseiq.SybaseIqParamReader;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import org.apache.commons.dbutils.QueryRunner;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SybaseIqDbMetadataManagerIT extends AbstractDbMetadataManagerIT {
    @Parameterized.Parameters
    public static Collection<Object[]> params() {
        return SybaseIqParamReader.getParamReader().getJdbcDsAndSchemaParams();
    }

    public SybaseIqDbMetadataManagerIT(DataSource dataSource, PhysicalSchema physicalSchema) {
        super(dataSource, physicalSchema);
    }

    @Override
    protected DbMetadataManager createMetadataManager() {
        return new IqDbPlatform().getDbMetadataManager();
    }

    @Override
    protected void setCurrentSchema(QueryRunner jdbc) throws Exception {
        // No need to do anything here; we login as the schema user itself, so the schema is automatically set
    }

    @Override
    protected String getDropSqlFile() {
        return "sybaseiq-test-drops.sql";
    }

    @Override
    protected String getAddSqlFile() {
        return "sybaseiq-test.sql";
    }

    @Override
    protected String convertName(String name) {
        return name;
    }

    @Override
    protected String get_SP1() {
        return "create PROCEDURE SP1() AS -- ensure that SP comment remains DELETE FROM TABLE_A DELETE FROM TABLE_A";
    }

    @Override
    protected String get_FUNC1() {
        return "create FUNCTION FUNC1() RETURNS INT AS -- ensure that func comment remains RETURN 10";
    }

    @Override
    protected String get_VIEW1() {
        return "create view dbdeploy03.VIEW1 as select * from dbdeploy03.METADATA_TEST_TABLE -- my comment";
    }

    @Override
    protected String get_INVALID_VIEW() {
        return "create view dbdeploy03.INVALID_VIEW as select * from dbdeploy03.INVALID_TABLE";
    }

    public static String createIqUrl(String host, int port) {
        return String.format("jdbc:sybase:Tds:%1$s:%2$s", host, port);
    }

    @Override
    protected boolean isPmdKnownBroken() {
        return true;
    }

    @Override
    protected OverLoadSupport isSpOverloadSupported() {
        return OverLoadSupport.NONE;
    }

    @Override
    protected String get_SP_WITH_OVERLOAD_3() {
        return "create PROCEDURE SP_WITH_OVERLOAD(@INVAL INT) AS -- NOTE - no procedure overloads supported in IQ DELETE FROM TABLE_A DELETE FROM TABLE_A";
    }

    @Override
    protected boolean isFuncOverloadSupported() {
        return false;
    }

    @Override
    protected String get_FUNC_WITH_OVERLOAD_3() {
        return "create FUNCTION FUNC_WITH_OVERLOAD (@var1 INT, @INVALSTR VARCHAR(32)) RETURNS INT AS RETURN 10";
    }

    @Override
    protected boolean isOnlySpSupported() {
        return true;
    }

    @Override
    protected boolean isSequenceSupported() {
        return false;
    }
}
