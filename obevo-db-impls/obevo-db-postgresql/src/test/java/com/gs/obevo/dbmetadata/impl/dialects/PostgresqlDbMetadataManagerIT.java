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

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.impl.platforms.postgresql.PostgreSqlDbPlatform;
import com.gs.obevo.db.impl.platforms.postgresql.PostgreSqlParamReader;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import org.apache.commons.dbutils.QueryRunner;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class PostgresqlDbMetadataManagerIT extends AbstractDbMetadataManagerIT {
    @Parameterized.Parameters
    public static Iterable<Object[]> params() {
        return PostgreSqlParamReader.getParamReader().getJdbcDsAndSchemaParams();
    }

    public PostgresqlDbMetadataManagerIT(DataSource dataSource, PhysicalSchema physicalSchema) {
        super(dataSource, physicalSchema);
    }

    @Override
    protected DbMetadataManager createMetadataManager() {
        return new PostgreSqlDbPlatform().getDbMetadataManager();
    }

    @Override
    protected String convertName(String name) {
        return name.toLowerCase();
    }

    @Override
    protected String get_FUNC1() {
        return "BEGIN -- ensure that func comment remains RETURN 1; END;";
    }

    @Override
    protected boolean isInvalidViewPossible() {
        return false;  //  postgresql does not allow invalid views (i.e. views that have dependent tables dropped first)
    }

    @Override
    boolean isUserTypeSupported() {
        return true;
    }

    @Override
    protected boolean isStoredProcedureSupported() {
        return false;  // postgresql only supports functions, not procedures separately. Metadata for procedures is stored as functions for now
    }

    @Override
    protected String get_VIEW1() {
        return "SELECT metadata_test_table.afield, metadata_test_table.bfield FROM metadata_test_table;";
    }

    @Override
    protected void setCurrentSchema(QueryRunner jdbc) throws Exception {
        jdbc.update("SET search_path TO " + getSchemaName());
    }

    @Override
    protected String getDropSqlFile() {
        return "postgresql-test-drops.sql";
    }

    @Override
    protected String getAddSqlFile() {
        return "postgresql-test.sql";
    }

    @Override
    protected String get_FUNC_WITH_OVERLOAD_1() {
        return "BEGIN RETURN 1; END;";
    }

    @Override
    protected String get_FUNC_WITH_OVERLOAD_2() {
        return "BEGIN RETURN 1; END;";
    }

    @Override
    protected String get_FUNC_WITH_OVERLOAD_3() {
        return "BEGIN RETURN 1; END;";
    }
}
