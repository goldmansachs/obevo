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
package com.gs.obevo.impl.reader;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import com.gs.obevo.api.appdata.doc.TextMarkupDocument;
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.ConfigurationConverter;
import org.apache.commons.configuration2.ConfigurationUtils;
import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.mutable.ListAdapter;

/**
 * Class to parse the content of a string into {@link PackageMetadata}.
 */
class PackageMetadataReader {
    private final TextMarkupDocumentReader textMarkupDocumentReader;

    PackageMetadataReader(TextMarkupDocumentReader textMarkupDocumentReader) {
        this.textMarkupDocumentReader = textMarkupDocumentReader;
    }

    public PackageMetadata getPackageMetadata(String fileContent) {
        TextMarkupDocument textMarkupDocument = textMarkupDocumentReader.parseString(fileContent, null);
        TextMarkupDocumentSection metadataSection = textMarkupDocument.findSectionWithElementName(TextMarkupDocumentReader.TAG_METADATA);
        String packageMetadataContent = textMarkupDocument.getSections()
                .select(_this -> _this.getName() == null)
                .toReversed()
                .collect(TextMarkupDocumentSection::getContent)
                .collect(StringFunctions.trim())
                .detect(StringPredicates.notEmpty());

        ImmutableMap<String, String> sourceEncodingsMap = getSourceEncodings(getConfig(packageMetadataContent));

        if (metadataSection != null || sourceEncodingsMap.notEmpty()) {
            return new PackageMetadata(metadataSection, sourceEncodingsMap);
        } else {
            return null;
        }
    }

    private ImmutableHierarchicalConfiguration getConfig(String configContent) {
        if (configContent == null) {
            return new BaseHierarchicalConfiguration();
        }

        Properties props = new Properties();
        try {
            props.load(new StringReader(configContent));
            return ConfigurationUtils.convertToHierarchical(ConfigurationConverter.getConfiguration(props));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ImmutableMap<String, String> getSourceEncodings(ImmutableHierarchicalConfiguration metadataConfig) {
        MutableList<ImmutableHierarchicalConfiguration> encodingConfigs = ListAdapter.adapt(metadataConfig.immutableChildConfigurationsAt("sourceEncodings"));
        MutableMap<String, String> encodingsMap = Maps.mutable.empty();

        for (ImmutableHierarchicalConfiguration encodingConfig : encodingConfigs) {
            String fileList = encodingConfig.getString("");
            for (String file : fileList.split(",")) {
                encodingsMap.put(file, encodingConfig.getRootElementName());
            }
        }

        return encodingsMap.toImmutable();
    }
}
