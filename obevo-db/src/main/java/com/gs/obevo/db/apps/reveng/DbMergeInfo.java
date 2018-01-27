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
package com.gs.obevo.db.apps.reveng;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

class DbMergeInfo {
    private final String name;
    private final File inputDir;
    private String driverClassName;
    private String url;
    private String username;
    private String password;
    private String physicalSchema;

    private DbMergeInfo(String name, File inputDir) {
        this.name = name;
        this.inputDir = inputDir;
    }

    public static MutableCollection<DbMergeInfo> parseFromProperties(Configuration config) {
        Set<String> dbs = new HashSet<String>(config.getList("instances"));

        MutableList<DbMergeInfo> dbMergeInfos = Lists.mutable.empty();
        for (String db : dbs) {
            Configuration subset = config.subset(db);
            if (subset.containsKey("inputDir")) {
                File inputDir = new File(subset.getString("inputDir"));
                DbMergeInfo mergeInfo = new DbMergeInfo(db, inputDir);
                if (subset.containsKey("driverClassName")) {
                    mergeInfo.setDriverClassName(subset.getString("driverClassName"));
                    mergeInfo.setUrl(subset.getString("url"));
                    mergeInfo.setUsername(subset.getString("username"));
                    mergeInfo.setPassword(subset.getString("password"));
                    mergeInfo.setPhysicalSchema(subset.getString("physicalSchema"));
                }

                dbMergeInfos.add(mergeInfo);
            }
        }

        return dbMergeInfos;
    }

    public String getName() {
        return this.name;
    }

    public File getInputDir() {
        return this.inputDir;
    }

    public String getDriverClassName() {
        return this.driverClassName;
    }

    private void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getUrl() {
        return this.url;
    }

    private void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return this.username;
    }

    private void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    private void setPassword(String password) {
        this.password = password;
    }

    public String getPhysicalSchema() {
        return this.physicalSchema;
    }

    private void setPhysicalSchema(String physicalSchema) {
        this.physicalSchema = physicalSchema;
    }
}
