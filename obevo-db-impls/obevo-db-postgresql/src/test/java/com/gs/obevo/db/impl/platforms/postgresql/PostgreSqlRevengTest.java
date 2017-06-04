package com.gs.obevo.db.impl.platforms.postgresql;

import java.io.File;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class PostgreSqlRevengTest {
    @Test
    public void testReverseEngineeringFromFile() throws Exception {
        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema("dbdeploy03");
        args.setGenerateBaseline(false);
        args.setDbHost("dbdeploybuild.c87tzbeo5ssa.us-west-2.rds.amazonaws.com");
        args.setDbPort(5432);
        args.setDbServer("dbdeploy");
        args.setUsername("deploybuilddbo");
        args.setPassword("deploybuilddb0");

        File outputDir = new File("./target/outputReveng");
        FileUtils.deleteDirectory(outputDir);
        args.setOutputPath(outputDir);

        args.setInputPath(new File("./src/test/resources/reveng/postgre-dbdeploy03.sql"));

        new PostgreSqlPgDumpReveng().reveng(args);

        if (true) {
            DbEnvironment prod = DbEnvironmentFactory.getInstance().readOneFromSourcePath(outputDir.getPath(), "prod");
            prod.setCleanBuildAllowed(true);
            prod.buildAppContext("deploybuilddbo", "deploybuilddb0")
                    .cleanEnvironment()
                    .deploy();
        }

    }
}
