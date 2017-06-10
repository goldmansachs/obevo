package com.gs.obevo.db.impl.platforms.mssql;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import org.junit.Ignore;
import org.junit.Test;

public class MsSqlRevengIT {
    @Ignore("Not yet trying this out")
    @Test
    public void verifyThatReverseEngineeredSqlCanBeDeployed() {
        DbEnvironment prod = DbEnvironmentFactory.getInstance().readOneFromSourcePath("./target/outputReveng", "prod");
        prod.setCleanBuildAllowed(true);
        prod.buildAppContext("deploybuilddbo", "deploybuilddb0")
                .cleanEnvironment()
                .deploy();
    }
}
