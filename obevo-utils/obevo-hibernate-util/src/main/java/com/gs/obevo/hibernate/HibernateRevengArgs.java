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

import java.io.File;

import com.gs.obevo.db.api.platform.DbPlatform;
import org.jetbrains.annotations.Nullable;

public class HibernateRevengArgs<T> {
    private final String schema;
    private final File outputPath;
    private final DbPlatform platform;
    private final Class hibernateDialectClass;
    private final T config;
    private boolean generateBaseline;
    @Nullable
    private String postCreateTableSql;
    private boolean generateForeignKeys = true;
    private boolean explicitSchemaRequired;

    public HibernateRevengArgs(String schema, File outputPath, DbPlatform platform, Class hibernateDialectClass, T config) {
        this.schema = schema;
        this.outputPath = outputPath;
        this.platform = platform;
        this.hibernateDialectClass = hibernateDialectClass;
        this.config = config;
    }

    public String getSchema() {
        return schema;
    }

    public File getOutputPath() {
        return outputPath;
    }

    public DbPlatform getPlatform() {
        return platform;
    }

    public Class getHibernateDialectClass() {
        return hibernateDialectClass;
    }

    public T getConfig() {
        return config;
    }

    public boolean isGenerateBaseline() {
        return generateBaseline;
    }

    public void setGenerateBaseline(boolean generateBaseline) {
        this.generateBaseline = generateBaseline;
    }

    public HibernateRevengArgs<T> withGenerateBaseline(boolean generateBaseline) {
        this.setGenerateBaseline(generateBaseline);
        return this;
    }

    @Nullable
    public String getPostCreateTableSql() {
        return postCreateTableSql;
    }

    @Nullable
    public void setPostCreateTableSql(String postCreateTableSql) {
        this.postCreateTableSql = postCreateTableSql;
    }

    public HibernateRevengArgs<T> withPostCreateTableSql(String postCreateTableSql) {
        this.setPostCreateTableSql(postCreateTableSql);
        return this;
    }

    public boolean isGenerateForeignKeys() {
        return generateForeignKeys;
    }

    public void setGenerateForeignKeys(boolean generateForeignKeys) {
        this.generateForeignKeys = generateForeignKeys;
    }

    public HibernateRevengArgs<T> withGenerateForeignKeys(boolean generateForeignKeys) {
        this.setGenerateForeignKeys(generateForeignKeys);
        return this;
    }

    public boolean isExplicitSchemaRequired() {
        return explicitSchemaRequired;
    }

    public void setExplicitSchemaRequired(boolean explicitSchemaRequired) {
        this.explicitSchemaRequired = explicitSchemaRequired;
    }

    public HibernateRevengArgs<T> withExplicitSchemaRequired(boolean explicitSchemaRequired) {
        this.setExplicitSchemaRequired(explicitSchemaRequired);
        return this;
    }
}
