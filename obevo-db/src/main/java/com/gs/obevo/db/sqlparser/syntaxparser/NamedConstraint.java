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
package com.gs.obevo.db.sqlparser.syntaxparser;

public class NamedConstraint extends Constraint {
    private String name;

    public NamedConstraint() {
    }

    public NamedConstraint(String name, String type, String clusteredClause, String columns, String postObjectClauses) {
        super(type, clusteredClause, columns, postObjectClauses);
        this.name = name;
    }

    public NamedConstraint(String name, String rawText) {
        super(rawText);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
