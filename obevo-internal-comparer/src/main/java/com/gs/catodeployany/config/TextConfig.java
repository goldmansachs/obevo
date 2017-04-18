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
package com.gs.catodeployany.config;

public class TextConfig extends DataSourceConfig {

    private static final long serialVersionUID = -4557675627395376394L;

    private String file;

    private String delimiter;

    public TextConfig() {
        super("noname");
    }

    public TextConfig(String nm) {
        super(nm);
    }

    public String getFile() {
        return this.file;
    }

    public void setFile(String file1) {
        this.file = file1;
    }

    public String getDelimiter() {
        return this.delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public String toString() {
        return "Name - " + this.getName() + ", File - " + this.file + ", Delimiter - " + this.delimiter;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        TextConfig cloneConfig = new TextConfig("Copy of " + this.getName());
        cloneConfig.setFile(this.getFile());
        cloneConfig.setDelimiter(this.getDelimiter());
        return cloneConfig;
    }
}
