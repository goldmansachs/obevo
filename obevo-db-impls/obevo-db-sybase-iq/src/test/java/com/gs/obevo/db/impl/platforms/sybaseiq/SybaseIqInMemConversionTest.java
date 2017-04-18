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
package com.gs.obevo.db.impl.platforms.sybaseiq;

import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.impl.platforms.h2.H2DbPlatform;
import com.gs.obevo.db.unittest.UnitTestDbBuilder;
import org.junit.Test;

/**
 * Smoke test to ensure that the Sybase IQ configurations work fine
 * <p/>
 * Very few tables here, just want an end-to-end run
 */
public class SybaseIqInMemConversionTest {
    @Test
    public void testH2Conversion() {
        DbDeployerAppContext builder = UnitTestDbBuilder.newBuilder()
                .setSourcePath("platforms/sybaseiq/system-config-inmem.xml")
                .setReferenceEnvName("h2test")
                .setDbPlatform(new H2DbPlatform())
                .setDbServer("iqh2test")
                .buildContext();
        builder.setupEnvInfra();
        builder.build();
    }

    @Test
    public void testHsqlConversion() {
        DbDeployerAppContext builder = UnitTestDbBuilder.newBuilder()
                .setSourcePath("platforms/sybaseiq/system-config-inmem.xml")
                .setReferenceEnvName("hsqltest")
                .setDbPlatform(new H2DbPlatform())
                .setDbServer("iqhsqltest")
                .buildContext();
        builder.setupEnvInfra();
        builder.build();
    }
}
