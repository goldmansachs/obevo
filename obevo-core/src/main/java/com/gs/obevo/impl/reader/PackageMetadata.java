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

import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;

/**
 * Metadata that is common to the files in a given directory.
 */
public class PackageMetadata {
    private final TextMarkupDocumentSection metadataSection;
    private final ImmutableMap<String, String> fileToEncodingMap;

    /**
     * Default constructor.
     *
     * @param metadataSection {@link #getMetadataSection()}
     * @param fileToEncodingMap {@link #getFileToEncodingMap()}
     */
    public PackageMetadata(TextMarkupDocumentSection metadataSection, ImmutableMap<String, String> fileToEncodingMap) {
        this.metadataSection = metadataSection;
        this.fileToEncodingMap = fileToEncodingMap != null ? fileToEncodingMap : Maps.immutable.<String, String>empty();
    }

    /**
     * The metadata that will be commonly applied to all files in the directory.
     */
    public TextMarkupDocumentSection getMetadataSection() {
        return metadataSection;
    }

    /**
     * Mapping of the files that need to be encoded to the charsets that they will be encoded with.
     */
    public ImmutableMap<String, String> getFileToEncodingMap() {
        return fileToEncodingMap;
    }
}
