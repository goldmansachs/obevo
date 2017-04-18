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
package com.gs.obevo.db.impl.core;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.impl.DeployMetricsCollector;
import com.gs.obevo.impl.MainInputReader;
import com.gs.obevo.impl.SourceChangeReader;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.eclipse.collections.api.block.predicate.Predicate;

public class DbInputReader extends MainInputReader<DbPlatform, DbEnvironment> {
    public DbInputReader(SourceChangeReader sourceChangeReader, Predicate<? super Change> dbChangeFilter, DeployMetricsCollector deployMetricsCollector) {
        super(sourceChangeReader, dbChangeFilter, deployMetricsCollector);
    }

    @Override
    protected void validateSetup() {
        super.validateSetup();
        validateProperDbUtilsVersion();
    }

    private void validateProperDbUtilsVersion() {
        Package dbutilsPackage = BasicRowProcessor.class.getPackage();
        String dbutilsVersion = dbutilsPackage.getSpecificationVersion();

        if (dbutilsVersion == null) {
            return;  // in case the jar is shaded, this information may not be available; hence, we are forced to return
        }
        String[] versionParts = dbutilsVersion.split("\\.");
        if (versionParts.length < 2) {
            throw new IllegalArgumentException("Improper dbutils version; must have at least two parts x.y; " + dbutilsVersion);
        }

        int majorVersion = Integer.valueOf(versionParts[0]).intValue();
        int minorVersion = Integer.valueOf(versionParts[1]).intValue();

        if (!(majorVersion >= 1 && minorVersion >= 6)) {
            throw new IllegalArgumentException("commons-dbutils:commons-dbutils version must be >= 1.6 to avoid bugs w/ JDBC getColumnName() usage in <= 1.5");
        }
    }

    @Override
    protected void logEnvironmentMetrics(DbEnvironment env) {
        super.logEnvironmentMetrics(env);
        getDeployMetricsCollector().addMetric("dbDataSourceName", env.getDbDataSourceName());
    }
}
