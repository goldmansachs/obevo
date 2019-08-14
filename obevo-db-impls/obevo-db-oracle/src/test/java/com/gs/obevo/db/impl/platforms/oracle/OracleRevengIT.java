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
package com.gs.obevo.db.impl.platforms.oracle;

import java.io.File;
import java.util.Set;

import javax.sql.DataSource;

import com.gs.obevo.api.factory.Obevo;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.apps.reveng.AquaRevengArgs;
import org.apache.commons.io.FileUtils;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class OracleRevengIT {
    private static final Logger LOG = LoggerFactory.getLogger(OracleRevengIT.class);
    private final Set<Integer> runSteps = Sets.mutable.of(1, 2);

    @Parameterized.Parameters
    public static Iterable<Object[]> params() {
        return OracleParamReader.getParamReader().getAppContextAndJdbcDsParams();
    }

    private final IntToObjectFunction<DbDeployerAppContext> getAppContext;
    private final DataSource ds;

    public OracleRevengIT(IntToObjectFunction<DbDeployerAppContext> getAppContext, DataSource ds) {
        this.getAppContext = getAppContext;
        this.ds = ds;
    }

    @Test
    public void testReveng() throws Exception {
        DbDeployerAppContext dbDeployerAppContext = getAppContext.valueOf(2);

        if (runSteps.contains(1)) {
            LOG.info("Stage 1 - do a deploy to setup the reverse-engineering input; use the latest step in the example directory");
            dbDeployerAppContext
                    .setupEnvInfra()
                    .cleanEnvironment()
                    .deploy()
            ;
        }

        DbEnvironment env = dbDeployerAppContext.getEnvironment();
        File outputDir = new File("./target/outputReveng");

        if (runSteps.contains(2)) {
            LOG.info("Stage 2 - perform the reverse-engineering and verify the output");
            FileUtils.deleteDirectory(outputDir);

            AquaRevengArgs args = new AquaRevengArgs();
            args.setOutputPath(outputDir);
            args.setDbSchema(env.getPhysicalSchema("schema1").getPhysicalName());
            args.setGenerateBaseline(false);
            args.setJdbcUrl(env.getJdbcUrl());
            args.setUsername(env.getDefaultUserId());
            args.setPassword(env.getDefaultPassword());

            new OracleReveng().reveng(args);

            OracleRevengTest.compareOutput(new File(outputDir, "final"));
        }

        if (runSteps.contains(3)) {
            LOG.info("Stage 3 - redeploy the reverse-engineered output to verify that we have a valid schema");
            DbEnvironment prod = Obevo.readEnvironment(new File(outputDir, "final").getPath(), "prod");
            prod.setCleanBuildAllowed(true);  // override this value programmatically as the reverse-engineered output sets this to false
            prod.buildAppContext(env.getDefaultUserId(), env.getDefaultPassword())
                    .cleanEnvironment()
                    .deploy();
        }
    }
}
