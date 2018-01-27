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
package com.gs.obevo.db.impl.platforms.sybasease;

import javax.sql.DataSource;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.h2.H2DbPlatform;
import com.gs.obevo.db.impl.platforms.hsql.HsqlDbPlatform;
import com.gs.obevo.db.unittest.UnitTestDbBuilder;
import com.gs.obevo.util.inputreader.Credential;
import org.junit.Test;

public class AseInMemConversionTest {

    /**
     * The key nuance in this test is that we go from Sybase ASE scripts (case-sensitive) to H2 (case-INsensitive)
     */
    @Test
    public void testH2Conversion() throws Exception {
        DbEnvironment dbEnv = UnitTestDbBuilder.newBuilder()
                .setSourcePath("platforms/sybasease/step1/system-config-inmem.xml")
                .setReferenceEnvName("unittest-h2")
                .setDbPlatform(new H2DbPlatform())
                .setDbServer("mytest")
                .buildContext()
                .setupEnvInfra()
                .deploy()
                .setupAndCleanAndDeploy()
                .getEnvironment();

        DataSource ds = JdbcDataSourceFactory.createFromJdbcUrl(
                org.h2.Driver.class,
                dbEnv.getJdbcUrl(),
                new Credential(dbEnv.getDefaultUserId(), dbEnv.getDefaultPassword())
        );

        JdbcHelper jdbc = new JdbcHelper();

        String schemaPrefix = dbEnv.getPlatform().getSchemaPrefix(dbEnv.getPhysicalSchema("oats"));
        SybaseAseDeployerMainIT.validateStep1(ds, jdbc, schemaPrefix);

        UnitTestDbBuilder.newBuilder()
                .setSourcePath("platforms/sybasease/step2/system-config-inmem.xml")
                .setReferenceEnvName("unittest-h2")
                .setDbPlatform(new H2DbPlatform())
                .setDbServer("mytest")
                .buildContext().setupAndCleanAndDeploy();

        SybaseAseDeployerMainIT.validateStep2(ds, jdbc, schemaPrefix);
    }

    @Test
    public void testHsqlConversion() throws Exception {
        DbEnvironment dbEnv = UnitTestDbBuilder.newBuilder()
                .setSourcePath("platforms/sybasease/step1/system-config-inmem.xml")
                .setReferenceEnvName("unittest-hsql")
                .setDbPlatform(new HsqlDbPlatform())
                .setDbServer("mytest")
                .buildContext()
                .setupEnvInfra()
                .cleanAndDeploy()
                .setupAndCleanAndDeploy()
                .getEnvironment();

        DataSource ds = JdbcDataSourceFactory.createFromJdbcUrl(
                org.hsqldb.jdbcDriver.class,
                dbEnv.getJdbcUrl(),
                new Credential(dbEnv.getDefaultUserId(), dbEnv.getDefaultPassword())
        );

        JdbcHelper jdbc = new JdbcHelper();
        String schemaPrefix = dbEnv.getPlatform().getSchemaPrefix(dbEnv.getPhysicalSchema("oats"));
        SybaseAseDeployerMainIT.validateStep1(ds, jdbc, schemaPrefix);

        UnitTestDbBuilder.newBuilder()
                .setSourcePath("platforms/sybasease/step2/system-config-inmem.xml")
                .setReferenceEnvName("unittest-hsql")
                .setDbPlatform(new HsqlDbPlatform())
                .setDbServer("mytest")
                .buildContext().setupAndCleanAndDeploy();

        SybaseAseDeployerMainIT.validateStep2(ds, jdbc, schemaPrefix);
    }
}
