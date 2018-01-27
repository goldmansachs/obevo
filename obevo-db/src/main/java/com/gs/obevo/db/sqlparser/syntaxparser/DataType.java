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

public class DataType {
    private String typeName;
    private String typeParams;

    public DataType() {
    }

    public DataType(String typeName, String typeParams) {
        this.typeName = typeName;
        this.typeParams = typeParams;
    }

    public String getTypeParams() {
        return this.typeParams;
    }

    public void setTypeParams(String typeParams) {
        this.typeParams = typeParams;
    }

    public DataType newWithTypeParams(String typeParams) {
        return new DataType(this.typeName, typeParams);
    }

    public String getTypeName() {
        return this.typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public DataType newWithTypeName(String typeName) {
        return new DataType(typeName, this.typeParams);
    }
}
