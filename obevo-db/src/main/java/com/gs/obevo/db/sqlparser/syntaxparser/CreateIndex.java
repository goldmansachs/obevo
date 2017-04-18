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
package com.gs.obevo.db.sqlparser.syntaxparser;

public class CreateIndex {
    private String name;
    private boolean unique;
    private String indexQualifier;
    private String tableName;
    private String columns;
    private String postCreateObjectClauses;
    private String clusterClause;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean isUnique() {
        return this.unique;
    }

    public void setIndexQualifier(String indexQualifier) {
        this.indexQualifier = indexQualifier;
    }

    public String getIndexQualifier() {
        return this.indexQualifier;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return this.tableName;
    }

    public void setColumns(String columns) {
        this.columns = columns;
    }

    public String getColumns() {
        return this.columns;
    }

    public void setPostCreateObjectClauses(String postCreateObjectClauses) {
        this.postCreateObjectClauses = postCreateObjectClauses;
    }

    public String getPostCreateObjectClauses() {
        return this.postCreateObjectClauses;
    }

    public void setClusterClause(String clusterClause) {
        this.clusterClause = clusterClause;
    }

    public String getClusterClause() {
        return this.clusterClause;
    }
}
