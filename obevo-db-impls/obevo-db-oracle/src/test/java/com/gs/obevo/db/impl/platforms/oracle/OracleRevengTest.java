package com.gs.obevo.db.impl.platforms.oracle;

import java.io.File;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class OracleRevengTest {
    @Test
    public void testReverseEngineeringFromFile() throws Exception {
        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema("DBDEPLOY03");
        //args.setInputPath(new File("./src/test/resources/reveng/db2look/input/db2input.txt"));
        args.setGenerateBaseline(false);
        args.setJdbcUrl("jdbc:oracle:thin:@dbdeploy-oracle-12-1.c87tzbeo5ssa.us-west-2.rds.amazonaws.com:1521:DBDEPLOY");
        args.setUsername("deploybuilddbo");
        args.setPassword("deploybuilddb0");

        File outputDir = new File("./target/outputReveng");
        FileUtils.deleteDirectory(outputDir);
        args.setOutputPath(outputDir);

        args.setInputPath(new File("./src/test/resources/reveng/output-dbdeploy03.sql"));

        new OracleReveng().reveng(args);

    }

    @Test
    public void testRedeploy() {
        File outputDir = new File("./target/outputReveng");
        DbEnvironment prod = DbEnvironmentFactory.getInstance().readOneFromSourcePath(outputDir.getPath(), "prod");
        prod.setCleanBuildAllowed(true);
        prod.buildAppContext("deploybuilddbo", "deploybuilddb0")
                .cleanEnvironment()
                .deploy();

    }
}
