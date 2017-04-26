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
package com.gs.obevocomparer.config;

public class DatabaseConfig extends DataSourceConfig {

    private static final long serialVersionUID = -4631143656309716907L;

    private String user;

    private String password;

    private String URL;

    private String query;

    public DatabaseConfig() {
        super("noname");
    }

    public DatabaseConfig(String name, String user, String password,
            String url, String query) {
        super(name);
        this.setUser(user);
        this.setPassword(password);
        this.setURL(url);
        this.setQuery(query);
    }

    public String getQuery() {
        return this.query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getURL() {
        return this.URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    @Override
    public String toString() {
        return "Name - " + this.getName() + ", User - " + this.user + ", URL - " + this.URL + ", Query - " + this.query;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        DatabaseConfig cloneConfig = new DatabaseConfig("Copy of " + this.getName(), this.user, this.password, this.URL, this.query);
        cloneConfig.setSorted(this.getSorted());
        return cloneConfig;
    }
}
