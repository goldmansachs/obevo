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
package com.gs.obevocomparer.config;

import java.util.ArrayList;
import java.util.List;

public class ReconConfig {

    private static final String FIELDS_DELIMITER = ",";

    private String name;

    private DataSourceConfig dataSource1;

    private DataSourceConfig dataSource2;

    private List<ReconFieldConfig> allFields;

    public ReconConfig() {
        this.init();
    }

    public ReconConfig(String nm, DataSourceConfig ds1, DataSourceConfig ds2) {
        this.init();
        this.setReconName(nm);
        this.setDataSource1(ds1);
        this.setDataSource2(ds2);
    }

    private void init() {
        this.allFields = new ArrayList<ReconFieldConfig>();
    }

    public String getReconName() {
        return this.name;
    }

    public void setReconName(String reconName) {
        this.name = reconName;
    }

    public DataSourceConfig getDataSource1() {
        return this.dataSource1;
    }

    public void setDataSource1(DataSourceConfig dataSource1) {
        this.dataSource1 = dataSource1;
    }

    public DataSourceConfig getDataSource2() {
        return this.dataSource2;
    }

    public void setDataSource2(DataSourceConfig dataSource2) {
        this.dataSource2 = dataSource2;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name - ").append(this.name);
        sb.append(", Data Source 1 - ").append(this.dataSource1.getName());
        sb.append(", Data Source 2 - ").append(this.dataSource2.getName());
        if (this.allFields.size() > 0) {
            for (ReconFieldConfig rfc : this.allFields) {
                sb.append("\n").append(rfc);
            }
        }
        return sb.toString();
    }

    public List<ReconFieldConfig> getAllFields() {
        return this.allFields;
    }

    public void addField(ReconFieldConfig rfc) {
        this.allFields.add(rfc);
    }

    public void removeAllFields() {
        this.allFields.clear();
    }

    public void setAllField(String fields, String keyFields,
            String attrbuteFields, String excludedFields) {
        String[] all = fields.split(FIELDS_DELIMITER);
        for (int i = 0; i < all.length; i++) {
            String keyName = all[i].trim();
            this.allFields.add(new ReconFieldConfig(keyName));
        }
        String[] keys = keyFields.split(FIELDS_DELIMITER);
        for (int i = 0; i < keys.length; i++) {
            this.getField(keys[i]).setKey(true);
        }
        String[] attributes = attrbuteFields.split(FIELDS_DELIMITER);
        for (int i = 0; i < attributes.length; i++) {
            this.getField(attributes[i]).setAttribute(true);
        }
        String[] excludes = excludedFields.split(FIELDS_DELIMITER);
        for (int i = 0; i < excludes.length; i++) {
            this.getField(excludes[i]).setExcluded(true);
        }
    }

    public ReconFieldConfig getField(String name) {
        for (ReconFieldConfig rfc : this.allFields) {
            if (rfc.getName().equals(name)) {
                return rfc;
            }
        }
        return null;
    }

    public List<String> getKeyFields() {
        List<String> keys = new ArrayList<String>();
        for (ReconFieldConfig rfc : this.allFields) {
            if (rfc.isKey()) {
                keys.add(rfc.getName());
            }
        }
        return keys;
    }

    public List<String> getFields() {
        List<String> attributes = new ArrayList<String>();
        for (ReconFieldConfig rfc : this.allFields) {
            attributes.add(rfc.getName());
        }
        return attributes;
    }

    public List<String> getExcludedFields() {
        List<String> excludes = new ArrayList<String>();
        for (ReconFieldConfig rfc : this.allFields) {
            if (rfc.isExcluded()) {
                excludes.add(rfc.getName());
            }
        }
        return excludes;
    }
}
