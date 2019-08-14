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

import com.gs.obevo.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.api.factory.DbPlatformConfiguration;
import com.gs.obevo.db.api.platform.DbPlatform;

/**
 * Converter for reladomo schema inputs into the Obevo format.
 *
 * Ideally, we would not use this adapter class and instead rely directly on {@link ReladomoDdlReveng}. However, this
 * API class existed before, so we keep it for backward-compatibility.
 */
public class ReladomoSchemaConverter {
    public void convertDdlsToDaFormat(ReladomoSchemaConverterArgs args) {
        convertDdlsToDaFormat(DbPlatformConfiguration.getInstance().valueOf(args.getPlatform()), args.getInputDir(), args.getOutputDir(), args.getDbSchema(), !args.isDontGenerateBaseline(), args.getExcludeObjects());
    }

    public void convertDdlsToDaFormat(final DbPlatform platform, File inputPath, File outputPath, final String dbSchema, boolean generateBaseline, String excludeObjects) {
        ReladomoDdlReveng reladomoDdlReveng = new ReladomoDdlReveng(platform);
        AquaRevengArgs args = new AquaRevengArgs();
        args.setInputPath(inputPath);
        args.setOutputPath(outputPath);
        args.setDbSchema(dbSchema);
        args.setGenerateBaseline(generateBaseline);
        args.setExcludeObjects(excludeObjects);
        reladomoDdlReveng.reveng(args);
    }
}
