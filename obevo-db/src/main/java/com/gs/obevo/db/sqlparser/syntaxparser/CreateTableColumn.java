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

public class CreateTableColumn {
    private String name;
    private DataType type = new DataType();
    private String postColumnText;

    public CreateTableColumn() {
    }

    public CreateTableColumn(String name, DataType type, String postColumnText) {
        this.name = name;
        this.type = type;
        this.postColumnText = postColumnText;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CreateTableColumn newWithName(String name) {
        return new CreateTableColumn(name, this.type, this.postColumnText);
    }

    public DataType getType() {
        return this.type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public CreateTableColumn newWithType(DataType type) {
        return new CreateTableColumn(this.name, type, this.postColumnText);
    }

    public String getPostColumnText() {
        return this.postColumnText;
    }

    public void setPostColumnText(String postColumnText) {
        this.postColumnText = postColumnText;
    }

    public CreateTableColumn newWithPostColumnText(String name) {
        return new CreateTableColumn(this.name, this.type, postColumnText);
    }
}
