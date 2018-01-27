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
package com.gs.obevo.db.impl.core.compare.data;

import java.sql.Driver;

import javax.sql.DataSource;

import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.util.inputreader.Credential;
import org.eclipse.collections.api.block.function.Function;

public class DbDataSource {
    public static final Function<DbDataSource, String> TO_NAME = new Function<DbDataSource, String>() {
        @Override
        public String valueOf(DbDataSource object) {
            return object.getName();
        }
    };

    private String name;
    private String url;
    private String schema;
    private String username;
    private String password;
    private String driverClassName;
    private DataSource ds;

    public void init() {
        if (this.ds == null) {
            try {
                Class<? extends Driver> driverClass = (Class<? extends Driver>) Class.forName(this.driverClassName);
                this.ds = JdbcDataSourceFactory.createFromJdbcUrl(driverClass, this.url, new Credential(this.username, this.password));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public DbDataSource() {
    }

    private DbDataSource(String name, String url, String schema, String username, String password) {
        this.name = name;
        this.url = url;
        this.schema = schema;
        this.username = username;
        this.password = password;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSchema() {
        return this.schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public DataSource getDs() {
        return this.ds;
    }

    public String getDriverClassName() {
        return this.driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public static class Builder {
        private String name;
        private String url;
        private String schema;
        private String username;
        private String password;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setSchema(String schema) {
            this.schema = schema;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public DbDataSource createDbDataSource() {
            return new DbDataSource(this.name, this.url, this.schema, this.username, this.password);
        }
    }
}
