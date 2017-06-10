package com.gs.obevo.db.impl.platforms.postgresql;

import java.io.File;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class PostgreSqlRevengTest {
    @Test
    public void testReverseEngineeringFromFile() throws Exception {
        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema("myschema01");
        args.setGenerateBaseline(false);
        args.setDbHost("myhost.me.com");
        args.setDbPort(1234);
        args.setDbServer("myserver");
        args.setUsername("myuser");
        args.setPassword("mypass");

        File outputDir = new File("./target/outputReveng");
        FileUtils.deleteDirectory(outputDir);
        args.setOutputPath(outputDir);

        args.setInputPath(new File("./src/test/resources/reveng/pgdump/input/input.sql"));

        new PostgreSqlPgDumpReveng().reveng(args);

        DirectoryAssert.assertDirectoriesEqual(new File("./src/test/resources/reveng/pgdump/expected"), outputDir);
    }
}
