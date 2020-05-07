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
package com.gs.obevo.db.impl.platforms.postgresql;

import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.impl.platforms.h2.H2DbPlatform;
import com.gs.obevo.db.impl.platforms.hsql.HsqlDbPlatform;
import com.gs.obevo.db.unittest.UnitTestDbBuilder;
import org.junit.Test;

public class PostgreSqlInMemConversionTest {
    @Test
    public void testInMemoryHsql() {
        DbDeployerAppContext context = UnitTestDbBuilder.newBuilder()
                .setSourcePath("platforms/postgresql/step1/system-config-inmem.xml")
                .setReferenceEnvName("unittestrefhsql")
                .setDbPlatform(new HsqlDbPlatform())
                .setDbServer("mydb2testHsql")
                .buildContext();
        context.setupEnvInfra();
        context.cleanAndDeploy();

        // TODO add assertions
    }

    // Implementation for H2 TBD
//    @Test
//    public void testInMemoryH2() {
//        DbDeployerAppContext context = UnitTestDbBuilder.newBuilder()
//                .setSourcePath("platforms/postgresql/step1/system-config-inmem.xml")
//                .setReferenceEnvName("unittestrefh2")
//                .setDbPlatform(new H2DbPlatform())
//                .setDbServer("mydb2testH2")
//                .buildContext();
//        context.setupEnvInfra();
//        context.cleanAndDeploy();
//
//        // TODO add assertions
//    }
}
