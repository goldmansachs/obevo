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

import com.sampullara.cli.Argument;

class DbDataComparisonArgs {
    private String configFile;
    private String outputDir;

    public String getConfigFile() {
        return this.configFile;
    }

    @Argument(value = "configFile", required = true, description = "config file path for the comparison. Can be on the classpath or file path")
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public String getOutputDir() {
        return this.outputDir;
    }

    @Argument(value = "outputDir", required = true, description = "outputDir where the reports will go")
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }
}
