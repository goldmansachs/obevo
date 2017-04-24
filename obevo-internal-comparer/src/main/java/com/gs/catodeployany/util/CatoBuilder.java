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
package com.gs.catodeployany.util;

import java.util.Collection;

import com.gs.catodeployany.compare.CatoComparison;
import com.gs.catodeployany.compare.CatoProperties;
import com.gs.catodeployany.compare.simple.SimpleCatoProperties;
import com.gs.catodeployany.config.CatoResources;
import com.gs.catodeployany.input.CatoDataSource;
import org.eclipse.collections.impl.list.mutable.FastList;

public class CatoBuilder {

    private CatoBuilder(CatoDataSource leftSource, CatoDataSource rightSource, CatoProperties properties) {
        this.leftSource = leftSource;
        this.rightSource = rightSource;
        this.properties = properties;
    }

    CatoDataSource leftSource;
    CatoDataSource rightSource;
    CatoProperties properties;
    CatoResources resource;

    private CatoBuilder() {

    }

    public static CatoBuilder newInstance() {
        return new CatoBuilder();
    }

    public CatoBuilder withSources(CatoDataSource leftSource, CatoDataSource rightSource) {
        this.leftSource = leftSource;
        this.rightSource = rightSource;
        return this;
    }

    public CatoBuilder withProperties(CatoProperties properties) {
        this.properties = properties;
        return this;
    }

    public CatoBuilder withFields(Collection<String> keyFields, Collection<String> excludeFields) {
        this.properties = new SimpleCatoProperties(FastList.newList(keyFields), excludeFields);
        return this;
    }

    public CatoBuilder withResource(CatoResources resource) {
        this.resource = resource;
        return this;
    }

    public CatoComparison compare(String name) {
        CatoComparison comparison = null;
        if (this.leftSource != null && this.rightSource != null && this.properties != null) {
            comparison = CatoBaseUtil.compare(name, this.leftSource, this.rightSource, this.properties);
        } else if (this.resource != null) {
            // ReconConfig config = this.resource.getRecon(name);
            // TODO Make DataSourceConfig implement CatoDataSource?
            // CatoUtil.compare(name, config.getDataSource1(), config.getDataSource2(),config.getKeyFields() ,
            // config.getExcludedFields());
            throw new UnsupportedOperationException("Need to implement this section; though this may be unused in Obevo codebase");
        }

        return comparison;
    }
}
