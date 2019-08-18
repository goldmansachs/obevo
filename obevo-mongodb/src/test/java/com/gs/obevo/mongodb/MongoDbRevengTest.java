package com.gs.obevo.mongodb;

import java.io.File;
import java.nio.charset.StandardCharsets;

import com.gs.obevo.apps.reveng.AbstractRevengTest;
import com.gs.obevo.apps.reveng.AquaRevengArgs;
import com.gs.obevo.mongodb.impl.MongoDbPlatform;
import com.gs.obevo.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class MongoDbRevengTest extends AbstractRevengTest {
    @Test
    @Override
    public void testReverseEngineeringFromFile() throws Exception {
        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema("mydb");
        args.setGenerateBaseline(false);
        args.setJdbcUrl(MongoDbTestHelper.CONNECTION_URI);
        args.setUsername("myuser");
        args.setPassword("mypass");
        args.setCharsetEncoding(StandardCharsets.UTF_8.displayName());

        File outputDir = new File("./target/outputReveng");
        FileUtils.deleteDirectory(outputDir);
        args.setOutputPath(outputDir);

//        args.setInputPath(new File("./src/test/resources/reveng/oracle/input.sql"));

        new MongoDbPlatform().getDdlReveng().reveng(args);

        compareOutput(outputDir);
    }

    /**
     * Compares the reverse-engineering output. Set as package-private so that the reverse-engineering integration test
     * can also access this.
     */
    static void compareOutput(File outputDir) {
        DirectoryAssert.assertDirectoriesEqual(new File("./src/test/resources/reveng/mongodb/expected"), outputDir, true);
    }
}
