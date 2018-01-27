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
package com.gs.obevo.db.impl.core.compare.data;

import java.io.File;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;

/**
 * TODO write a unit test for this
 */
class DbDataComparisonExample {
    public static void main(String[] args) throws Exception {
        new DbDataComparisonUtil().execute(DbDataComparisonConfigFactory
                .createFromProperties("dbComparisonConfigExample.properties"), new File("./comps"));
    }

    public static void option2() throws Exception {
        Class.forName("com.sybase.jdbc3.jdbc.SybDriver");

        DbDataComparisonConfig reconConfig = new DbDataComparisonConfig();
        MutableList<DbDataSource> dbDataSources = Lists.mutable.with(
                new DbDataSource.Builder().setName("dev1").setUrl("jdbc:sybase:Tds:myhost1.me.com:1234")
                        .setSchema("schemaA").setUsername("user1").setPassword("NOT_A_PASSWORD")
                        .createDbDataSource()
                ,
                new DbDataSource.Builder().setName("uat").setUrl("jdbc:sybase:Tds:myhost2.me.com:1234")
                        .setSchema("schemaB").setUsername("user2").setPassword("NOT_A_PASSWORD")
                        .createDbDataSource()
                ,
                new DbDataSource.Builder().setName("prod1")
                        .setUrl("jdbc:sybase:Tds:myhost3.me.com:1234")
                        .setSchema("schemaC").setUsername("user3").setPassword("NOT_A_PASSWORD")
                        .createDbDataSource()
                ,
                new DbDataSource.Builder().setName("prod2")
                        .setUrl("jdbc:sybase:Tds:myhost4.me.com:1234")
                        .setSchema("schemaD").setUsername("user4").setPassword("NOT_A_PASSWORD")
                        .createDbDataSource()
        );
        reconConfig.setDbDataSources(dbDataSources);
        reconConfig.setComparisonCommandNamePairs(Lists.mutable.with(
                Tuples.pair("dev1", "uat")
                , Tuples.pair("uat", "prod1")
                , Tuples.pair("dev1", "prod2")
        ));
        reconConfig.setInputTables(Lists.mutable.of("TableA", "TableB", "TableC", "TableD"));
        reconConfig.setExcludedTables(Sets.mutable.of("ExcludedTableA", "ExcludedTableB"));
        new DbDataComparisonUtil().execute(reconConfig, new File("./comps"));
    }
}
