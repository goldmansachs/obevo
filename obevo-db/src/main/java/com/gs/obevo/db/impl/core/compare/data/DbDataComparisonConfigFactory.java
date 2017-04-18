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
package com.gs.obevo.db.impl.core.compare.data;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

public class DbDataComparisonConfigFactory {
    public static DbDataComparisonConfig createFromProperties(String path) {
        try {
            URL url = DbDataComparisonConfigFactory.class.getClassLoader().getResource(path);
            if (url == null) {
                url = new File(path).toURI().toURL();
            }
            if (url == null) {
                throw new IllegalArgumentException("Could not find resource or file at path: " + path);
            }

            return createFromProperties(new PropertiesConfiguration(url));
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Could not find resource or file at path: " + path, e);
        }
    }

    public static DbDataComparisonConfig createFromProperties(final Configuration config) {
        Properties propsView = ConfigurationConverter.getProperties(config);  // config.getString() automatically parses
        // for commas...would like to avoid this
        DbDataComparisonConfig compConfig = new DbDataComparisonConfig();
        compConfig.setInputTables(Lists.mutable.with(propsView.getProperty("tables.include").split(",")));
        compConfig.setExcludedTables(Lists.mutable.with(propsView.getProperty("tables.exclude").split(",")).toSet());
        String comparisonsStr = propsView.getProperty("comparisons");

        MutableList<Pair<String, String>> compCmdPairs = Lists.mutable.empty();
        MutableSet<String> dsNames = UnifiedSet.newSet();
        for (String compPairStr : comparisonsStr.split(";")) {
            String[] pairParts = compPairStr.split(",");
            compCmdPairs.add(Tuples.pair(pairParts[0], pairParts[1]));

            // note - if I knew where the Pair.TO_ONE TO_TWO selectors were, I'd use those
            dsNames.add(pairParts[0]);
            dsNames.add(pairParts[1]);
        }

        compConfig.setComparisonCommandNamePairs(compCmdPairs);

        MutableList<DbDataSource> dbDataSources = dsNames.toList().collect(new Function<String, DbDataSource>() {
            @Override
            public DbDataSource valueOf(String dsName) {
                Configuration dsConfig = config.subset(dsName);

                DbDataSource dbDataSource = new DbDataSource();
                dbDataSource.setName(dsName);
                dbDataSource.setUrl(dsConfig.getString("url"));
                dbDataSource.setSchema(dsConfig.getString("schema"));
                dbDataSource.setUsername(dsConfig.getString("username"));
                dbDataSource.setPassword(dsConfig.getString("password"));
                dbDataSource.setDriverClassName(dsConfig.getString("driverClass"));

                return dbDataSource;
            }
        });
        compConfig.setDbDataSources(dbDataSources);
        return compConfig;
    }
}
