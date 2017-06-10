package com.gs.obevo.db.impl.platforms.mssql;

import java.io.File;

import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class MsSqlRevengTest {
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

        args.setInputPath(new File("./src/test/resources/reveng/input"));

        new MsSqlReveng().reveng(args);

        DirectoryAssert.assertDirectoriesEqual(new File("./src/test/resources/reveng/expected"), outputDir);
    }
}
