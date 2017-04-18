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

public class Constraint {
    private String type;
    private String clusteredClause;
    private String columns;
    private String postObjectClauses;
    private String rawText;

    public Constraint() {
    }

    public Constraint(String type, String clusteredClause, String columns, String postObjectClauses) {
        this.type = type;
        this.clusteredClause = clusteredClause;
        this.columns = columns;
        this.postObjectClauses = postObjectClauses;
    }

    public Constraint(String rawText) {
        this.rawText = rawText;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClusteredClause() {
        return this.clusteredClause;
    }

    public void setClusteredClause(String clusteredClause) {
        this.clusteredClause = clusteredClause;
    }

    public String getColumns() {
        return this.columns;
    }

    public void setColumns(String columns) {
        this.columns = columns;
    }

    public String getPostObjectClauses() {
        return this.postObjectClauses;
    }

    public void setPostObjectClauses(String postObjectClauses) {
        this.postObjectClauses = postObjectClauses;
    }

    public String getRawText() {
        return this.rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}
