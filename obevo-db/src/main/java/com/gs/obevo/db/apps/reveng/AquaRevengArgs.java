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
package com.gs.obevo.db.apps.reveng;

import java.io.File;
import java.util.Arrays;

import com.gs.obevo.db.api.factory.DbPlatformConfiguration;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.sampullara.cli.Argument;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

public class AquaRevengArgs {
    private File inputPath;
    private String[] tables;
    private File outputPath;
    private boolean tablespaceToken;
    private boolean tokenizeDefaultSchema;
    private boolean generateBaseline;
    private DbPlatform dbPlatform;
    private String dbHost;
    private String jdbcUrl;
    private Integer dbPort;
    private String dbServer;
    private String dbSchema;
    private String username;
    private String password;
    private String driverClass;
    private RevengMode mode;
    private MutableSet<String> updateTimeColumns;
    private String nameCombinePattern;
    private boolean preprocessSchemaTokens = true;
    private String excludeObjects;

    /**
     * @deprecated Use {@link #getInputPath()}
     */
    @Deprecated
    public File getInputDir() {
        return this.getInputPath();
    }

    /**
     * @deprecated Use {@link #setInputPath(File)}
     */
    @Deprecated
    @Argument(value = "inputDir", required = false)
    public void setInputDir(File inputDir) {
        this.setInputPath(inputDir);
    }

    public File getInputPath() {
        return inputPath;
    }

    @Argument(value = "inputPath", required = false)
    public void setInputPath(File inputPath) {
        this.inputPath = inputPath;
    }

    public String[] getTables() {
        return this.tables;
    }

    @Argument(value = "tables", required = false, description = "tables to reverse-engineer for data (comma-separated)")
    public void setTables(String[] tables) {
        this.tables = tables;
    }

    /**
     * @deprecated Use {@link #getOutputPath()}
     */
    @Deprecated
    public File getOutputDir() {
        return this.getOutputPath();
    }

    /**
     * @deprecated Use {@link #setOutputPath(File)}
     */
    @Deprecated
    @Argument(value = "outputDir", required = false)
    public void setOutputDir(File outputDir) {
        this.setOutputPath(outputDir);
    }

    public File getOutputPath() {
        return this.outputPath;
    }

    @Argument(value = "outputPath", required = false)
    public void setOutputPath(File outputPath) {
        this.outputPath = outputPath;
    }

    @Argument(value = "tablespaceToken", required = false)
    public void setTablespaceToken(boolean tablespaceToken) {
        this.tablespaceToken = tablespaceToken;
    }

    public boolean getTablespaceToken() {
        return this.tablespaceToken;
    }

    @Argument(value = "tokenizeDefaultSchema", required = false)
    public void setTokenizeDefaultSchema(boolean tokenizeDefaultSchema) {
        this.tokenizeDefaultSchema = tokenizeDefaultSchema;
    }

    public boolean getTokenizeDefaultSchema() {
        return this.tokenizeDefaultSchema;
    }

    public boolean isGenerateBaseline() {
        return this.generateBaseline;
    }

    @Argument(value = "generateBaseline", required = false)
    public void setGenerateBaseline(boolean generateBaseline) {
        this.generateBaseline = generateBaseline;
    }

    public DbPlatform getDbPlatform() {
        return this.dbPlatform;
    }

    @Argument(value = "dbType", required = true, description = "DB Type is needed to facilitate reverse engineering; use values [H2, HSQL, SYBASE_IQ, DB2, SYBASE_ASE, POSTGRESQL]")
    public void setDbTypeStr(String dbType) {
        this.dbPlatform = DbPlatformConfiguration.getInstance().valueOf(dbType.toUpperCase());
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    @Argument(value = "jdbcUrl", required = false, description = "Only for static data reverse-engineering")
    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getDbHost() {
        return this.dbHost;
    }

    @Argument(value = "dbHost", required = false, description = "Only for static data reverse-engineering")
    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public Integer getDbPort() {
        return this.dbPort;
    }

    @Argument(value = "dbPort", required = false, description = "Only for static data reverse-engineering")
    public void setDbPort(Integer dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbServer() {
        return this.dbServer;
    }

    @Argument(value = "dbServer", required = false, description = "Only for static data reverse-engineering")
    public void setDbServer(String dbServer) {
        this.dbServer = dbServer;
    }

    public String getDbSchema() {
        return this.dbSchema;
    }

    @Argument(value = "dbSchema", required = false, description = "For static data reverse-engineering or when pre-processing schema tokens (i.e. most uses)")
    public void setDbSchema(String dbSchema) {
        this.dbSchema = dbSchema;
    }

    public String getUsername() {
        return this.username;
    }

    @Argument(value = "username", required = false, description = "Only for static data reverse-engineering; if not passed in for reveng, the user will be prompted")
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    @Argument(value = "password", required = false, description = "Only for static data reverse-engineering; if not passed in for reveng, the user will be prompted")
    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClass() {
        return driverClass;
    }

    @Argument(value = "driverClass", required = false, description = "Driver class to use if the default doesn't work")
    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public RevengMode getMode() {
        return this.mode;
    }

    @Argument(value = "mode", required = true, description = "SCHEMA or DATA")
    public void setModeStr(String mode) {
        this.mode = RevengMode.valueOf(mode.toUpperCase());
    }

    public MutableSet<String> getUpdateTimeColumns() {
        return this.updateTimeColumns;
    }

    @Argument(value = "updateTimeColumns", required = false, description = "Only for static data reverse-engineering; optional - specifies any column names we should mark as updateTimeColumns")
    public void setUpdateTimeColumnsStr(String updateTimeColumns) {
        this.updateTimeColumns = UnifiedSet.newSetWith(StringUtils.split(updateTimeColumns, ","));
    }

    public String getNameCombinePattern() {
        return this.nameCombinePattern;
    }

    @Argument(value = "nameCombinePattern", required = false, description = "nameCombinePattern, must have {} in it")
    public void setNameCombinePattern(String nameCombinePattern) {
        this.nameCombinePattern = nameCombinePattern;
    }

    public boolean isPreprocessSchemaTokens() {
        return preprocessSchemaTokens;
    }

    @Argument(value = "preprocessSchemaTokens", required = false, description = "Specifies if we try to replace the existing schema references with blanks for easier cross-schema compatibility of scripts. Default is true; only set to false if the schema prefixes had already been stripped out previously.")
    public void setPreprocessSchemaTokensStr(String preprocessSchemaTokens) {
        this.preprocessSchemaTokens = Boolean.valueOf(preprocessSchemaTokens);
    }

    public String getExcludeObjects() {
        return excludeObjects;
    }

    @Argument(value = "excludeObjects", required = false, description = "Object patterns to exclude from reverse-engineering, e.g. TABLE~tab1,tab2;VIEW~view1,view2")
    public void setExcludeObjects(String excludeObjects) {
        this.excludeObjects = excludeObjects;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AquaRevengArgs [");
        if (inputPath != null) {
            builder.append("inputPath=").append(inputPath).append(", ");
        }
        if (tables != null) {
            builder.append("tables=").append(Arrays.toString(tables)).append(", ");
        }
        if (outputPath != null) {
            builder.append("outputPath").append(outputPath).append(", ");
        }
        builder.append("tablespaceToken=").append(tablespaceToken).append(", tokenizeDefaultSchema=")
                .append(tokenizeDefaultSchema).append(", generateBaseline=").append(generateBaseline).append(", ");
        if (dbPlatform != null) {
            builder.append("dbPlatform=").append(dbPlatform).append(", ");
        }
        if (dbHost != null) {
            builder.append("dbHost=").append(dbHost).append(", ");
        }
        builder.append("dbPort=").append(dbPort).append(", ");
        if (dbServer != null) {
            builder.append("dbServer=").append(dbServer).append(", ");
        }
        if (dbSchema != null) {
            builder.append("dbSchema=").append(dbSchema).append(", ");
        }
        if (username != null) {
            builder.append("username=").append(username).append(", ");
        }
        if (driverClass != null) {
            builder.append("driverClass=").append(driverClass).append(", ");
        }
        if (mode != null) {
            builder.append("mode=").append(mode).append(", ");
        }
        if (updateTimeColumns != null) {
            builder.append("updateTimeColumns=").append(updateTimeColumns).append(", ");
        }
        if (nameCombinePattern != null) {
            builder.append("nameCombinePattern=").append(nameCombinePattern).append(", ");
        }
        builder.append("preprocessSchemaTokens=").append(preprocessSchemaTokens).append("]");
        return builder.toString();
    }
}
