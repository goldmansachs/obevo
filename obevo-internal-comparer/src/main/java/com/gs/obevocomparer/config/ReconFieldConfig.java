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

public class ReconFieldConfig {

    private String name;
    private boolean isKey;
    private boolean isAttribute;
    private boolean isExcluded;

    public ReconFieldConfig(String name) {
        this.name = name;
    }

    public void setFlags(boolean key, boolean attribute, boolean excluded) {
        this.setKey(key);
        this.setAttribute(attribute);
        this.setExcluded(excluded);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isKey() {
        return this.isKey;
    }

    public void setKey(boolean isKey) {
        this.isKey = isKey;
    }

    public boolean isAttribute() {
        return this.isAttribute;
    }

    public void setAttribute(boolean isAttribute) {
        this.isAttribute = isAttribute;
    }

    public boolean isExcluded() {
        return this.isExcluded;
    }

    public void setExcluded(boolean isExcluded) {
        this.isExcluded = isExcluded;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name - ").append(this.name).append("[");
        if (this.isKey) {
            sb.append("key ");
        }
        if (this.isAttribute) {
            sb.append("attribute ");
        }
        if (this.isExcluded) {
            sb.append("excluded ");
        }
        sb.append("]");
        return sb.toString();
    }
}
