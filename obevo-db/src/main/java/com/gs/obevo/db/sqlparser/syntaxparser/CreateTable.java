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

import java.util.ArrayList;
import java.util.List;

public class CreateTable {
    private String name;
    private final List<CreateTableColumn> columns = new ArrayList<CreateTableColumn>();
    private String postTableCreateText;
    private final List<Constraint> constraints = new ArrayList<Constraint>();

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPostTableCreateText() {
        return this.postTableCreateText;
    }

    public List<CreateTableColumn> getColumns() {
        return this.columns;
    }

    public void setPostTableCreateText(String postTableCreateText) {
        this.postTableCreateText = postTableCreateText;
    }

    public List<Constraint> getConstraints() {
        return this.constraints;
    }
}
