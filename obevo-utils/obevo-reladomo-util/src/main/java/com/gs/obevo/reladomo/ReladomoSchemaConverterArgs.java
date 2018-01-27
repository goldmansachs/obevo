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
package com.gs.obevo.reladomo;

import java.io.File;

import com.sampullara.cli.Argument;

public class ReladomoSchemaConverterArgs {
    private File inputDir;
    private File outputDir;
    private boolean dontGenerateBaseline;
    private String dbSchema = "yourSchema";
    private String platform;
    private String excludeObjects;

    public File getInputDir() {
        return this.inputDir;
    }

    @Argument(value = "inputDir", required = true, description = "Input dir containing the DDLs")
    public void setInputDir(File inputDir) {
        this.inputDir = inputDir;
    }

    public File getOutputDir() {
        return this.outputDir;
    }

    @Argument(value = "outputDir", required = true, description = "Output dir to write the Obevo DDLs")
    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public boolean isDontGenerateBaseline() {
        return this.dontGenerateBaseline;
    }

    @Argument(value = "dontGenerateBaseline", required = false, description = "Whether to generate the associated baseline file")
    public void setDontGenerateBaseline(boolean dontGenerateBaseline) {
        this.dontGenerateBaseline = dontGenerateBaseline;
    }

    public String getDbSchema() {
        return this.dbSchema;
    }

    @Argument(value = "dbSchema", required = false, description = "Schema name to generate for the folder. Defaults to yourSchema")
    public void setDbSchema(String dbSchema) {
        this.dbSchema = dbSchema;
    }

    public String getPlatform() {
        return platform;
    }

    @Argument(value = "dbType", required = true, description = "DB Platform type where the Reladomo DDLs were generated for. Pick from the values: [H2, HSQL, SYBASE_IQ, DB2, SYBASE_ASE, POSTGRESQL]")
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getExcludeObjects() {
        return excludeObjects;
    }

    @Argument(value = "excludeObjects", required = false, description = "Object patterns to exclude from reverse-engineering, e.g. TABLE~tab1,tab2;VIEW~view1,view2")
    public void setExcludeObjects(String excludeObjects) {
        this.excludeObjects = excludeObjects;
    }
}
