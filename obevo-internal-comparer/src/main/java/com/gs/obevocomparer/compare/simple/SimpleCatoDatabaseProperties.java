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
package com.gs.obevocomparer.compare.simple;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.gs.obevocomparer.compare.CatoDatabaseProperties;

public class SimpleCatoDatabaseProperties extends SimpleCatoProperties implements CatoDatabaseProperties {

    public SimpleCatoDatabaseProperties(List<String> keyFields, String compareQuery, DataSource compareConnSource,
            String refQuery, DataSource refConnSource, String fileName, Map<String, String> mappedFields,
            boolean shouldUseStringTypeConverted, boolean isAlreadySorted) {
        super(keyFields);
        this.compareQuery = compareQuery;
        this.compareConnSource = compareConnSource;
        this.referenceQuery = refQuery;
        this.referenceConnSource = refConnSource;
        this.fileName = fileName;
        this.shouldUseStringTypeConverted = shouldUseStringTypeConverted;
        this.isAlreadySorted = isAlreadySorted;
    }

    public SimpleCatoDatabaseProperties(List<String> keyFields) {
        super(keyFields);
    }

    public SimpleCatoDatabaseProperties(List<String> keyFields, List<String> excludedFields) {
        super(keyFields, excludedFields);
    }

    public SimpleCatoDatabaseProperties() {
        super(Collections.<String>emptyList());
    }

    public String getCompareQuery() {
        return this.compareQuery;
    }

    public DataSource getCompareConnSource() {
        return this.compareConnSource;
    }

    public String getReferenceQuery() {
        return this.referenceQuery;
    }

    public DataSource getReferenceConnSource() {
        return this.referenceConnSource;
    }

    public String getFileName() {
        return this.fileName;
    }

    public boolean shouldUseStringTypeConverter() {

        return this.shouldUseStringTypeConverted;
    }

    public boolean isAlreadySorted() {

        return this.isAlreadySorted;
    }

    public boolean isShouldUseStringTypeConverted() {
        return this.shouldUseStringTypeConverted;
    }

    public void setShouldUseStringTypeConverted(boolean shouldUseStringTypeConverted) {
        this.shouldUseStringTypeConverted = shouldUseStringTypeConverted;
    }

    public void setCompareQuery(String compareQuery) {
        this.compareQuery = compareQuery;
    }

    public void setCompareConnSource(DataSource compareConnSource) {
        this.compareConnSource = compareConnSource;
    }

    public void setReferenceQuery(String referenceQuery) {
        this.referenceQuery = referenceQuery;
    }

    public void setReferenceConnSource(DataSource referenceConnSource) {
        this.referenceConnSource = referenceConnSource;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setAlreadySorted(boolean isAlreadySorted) {
        this.isAlreadySorted = isAlreadySorted;
    }

    private String compareQuery;
    private DataSource compareConnSource;
    private String referenceQuery;
    private DataSource referenceConnSource;

    private String fileName;

    private boolean shouldUseStringTypeConverted = true;
    private boolean isAlreadySorted = true;
    private List<String> queryParameters;

    public void setQueryParameters(List<String> queryParameters) {
        this.queryParameters = queryParameters;
    }

    @Override
    public List<String> getQueryParameters() {

        return this.queryParameters;
    }
}
