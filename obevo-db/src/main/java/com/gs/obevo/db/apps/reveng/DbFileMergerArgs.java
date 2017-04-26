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
package com.gs.obevo.db.apps.reveng;

import java.io.File;

import com.sampullara.cli.Argument;

public class DbFileMergerArgs {
    private File dbMergeConfigFile;
    private File outputDir;

    public File getDbMergeConfigFile() {
        return this.dbMergeConfigFile;
    }

    @Argument(value = "dbMergeConfigFile", required = true, description = "Config file w/ the DB entries to merge. " +
            "See the wiki for info on setting this up")
    public void setDbMergeConfigFile(File dbMergeConfigFile) {
        this.dbMergeConfigFile = dbMergeConfigFile;
    }

    public File getOutputDir() {
        return this.outputDir;
    }

    @Argument(value = "outputDir", required = true, description = "The folder where you want the merged results to go")
    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }
}
