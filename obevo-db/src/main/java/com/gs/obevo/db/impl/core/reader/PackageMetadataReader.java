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
package com.gs.obevo.db.impl.core.reader;

import com.gs.obevo.api.appdata.doc.TextMarkupDocument;
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import com.gs.obevo.util.vfs.FileObject;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

public class PackageMetadataReader {
    private final ConcurrentMutableMap<FileObject, TextMarkupDocumentSection> cache = new ConcurrentHashMap<FileObject, TextMarkupDocumentSection>();
    private final TextMarkupDocumentReader textMarkupDocumentReader;

    public PackageMetadataReader(TextMarkupDocumentReader textMarkupDocumentReader) {
        this.textMarkupDocumentReader = textMarkupDocumentReader;
    }

    public TextMarkupDocumentSection getPackageMetadata(final FileObject file) {
        return cache.getIfAbsentPut(file.getParent(), new Function0<TextMarkupDocumentSection>() {
            @Override
            public TextMarkupDocumentSection value() {
                FileObject packageMetadataFile = file.getParent().getChild("package-info.txt");

                // we check for containsKey, as we may end up persisting null as the value in the map
                if (packageMetadataFile == null || !packageMetadataFile.isReadable()) {
                    return null;
                } else {
                    TextMarkupDocument textMarkupDocument = textMarkupDocumentReader.parseString(packageMetadataFile.getStringContent(), null);
                    return textMarkupDocument.findSectionWithElementName(TextMarkupDocumentReader.TAG_METADATA);
                }
            }
        });
    }
}
