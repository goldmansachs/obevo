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
package com.gs.obevo.hibernate;

import javax.persistence.Column;
import javax.persistence.Id;

import org.hibernate.annotations.Index;

/**
 *
 */
@javax.persistence.Entity
@javax.persistence.Table(catalog = "mycat")
@org.hibernate.annotations.Table(appliesTo = "HibClassSchemaC", indexes = @Index(name = "Idx1", columnNames = {"col1", "col2"}))
public class HibClassSchemaC implements java.io.Serializable {
    @Id
    private int id;
    @Column(name = "name")
    private String name;
    @Column(name = "col1")
    private String col1;
    @Column(name = "col2")
    private String col2;

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
