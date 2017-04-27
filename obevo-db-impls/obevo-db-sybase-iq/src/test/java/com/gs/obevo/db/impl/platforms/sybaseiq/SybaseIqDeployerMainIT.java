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

import java.util.Collection;

import javax.sql.DataSource;

import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Smoke test to ensure that the Sybase IQ configurations work fine
 *
 * Very few tables here, just want an end-to-end run
 */
@RunWith(Parameterized.class)
public class SybaseIqDeployerMainIT {
    @Parameterized.Parameters
    public static Collection<Object[]> params() {
        return SybaseIqParamReader.getParamReader().getAppContextAndJdbcDsParams();
    }

    private final IntToObjectFunction<DbDeployerAppContext> getAppContext;
    private final DataSource ds;

    public SybaseIqDeployerMainIT(IntToObjectFunction<DbDeployerAppContext> getAppContext, DataSource ds) {
        this.getAppContext = getAppContext;
        this.ds = ds;
    }

    @Test
    public void testIQ_Ver_15_4() {
        DbDeployerAppContext dbDeployerAppContext = getAppContext.valueOf(1);
        dbDeployerAppContext
                .cleanEnvironment()
                .setupEnvInfra()
                .deploy();

        // TODO add an assertion against the DB that a table did get created
    }
}
