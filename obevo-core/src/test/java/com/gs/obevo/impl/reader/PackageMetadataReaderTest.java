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
package com.gs.obevo.impl.reader;

import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PackageMetadataReaderTest {

    @Test
    public void testPackageMetadataWithMetadata() {
        PackageMetadataReader packageMetadataReader = new PackageMetadataReader(new TextMarkupDocumentReader(false));

        PackageMetadata packageMetadata = packageMetadataReader.getPackageMetadata("\n\n  \n  \n" +
                "//// METADATA k1=v1 k2=v2 toggle1 toggle2\n" +
                "\n");

        assertEquals(Maps.immutable.of("k1", "v1", "k2", "v2"), packageMetadata.getMetadataSection().getAttrs());
        assertEquals(Sets.immutable.of("toggle1", "toggle2"), packageMetadata.getMetadataSection().getToggles());

        assertEquals(Maps.immutable.<String, String>empty(), packageMetadata.getFileToEncodingMap());
    }

    @Test
    public void testPackageMetadataWithProperties() {
        PackageMetadataReader packageMetadataReader = new PackageMetadataReader(new TextMarkupDocumentReader(false));

        PackageMetadata packageMetadata = packageMetadataReader.getPackageMetadata("\n\n  \n  \n" +
                "sourceEncodings.UTF-8=a1,a2,a3\n" +
                "sourceEncodings.UTF-16=a4\n" +
                "otherProps=abc\n" +
                "\n");

        assertNull(packageMetadata.getMetadataSection());

        assertEquals(Maps.immutable.of(
                "a1", "UTF-8",
                "a2", "UTF-8",
                "a3", "UTF-8",
                "a4", "UTF-16"
                )
                , packageMetadata.getFileToEncodingMap());
    }

    @Test
    public void testPackageMetadataWithMetadataAndProperties() {
        PackageMetadataReader packageMetadataReader = new PackageMetadataReader(new TextMarkupDocumentReader(false));

        PackageMetadata packageMetadata = packageMetadataReader.getPackageMetadata("\n\n  \n  \n" +
                "//// METADATA k1=v1 k2=v2 toggle1 toggle2\n" +
                "sourceEncodings.UTF-8=a1,a2,a3\n" +
                "sourceEncodings.UTF-16=a4\n" +
                "otherProps=abc\n" +
                "\n");

        assertEquals(Maps.immutable.of("k1", "v1", "k2", "v2"), packageMetadata.getMetadataSection().getAttrs());
        assertEquals(Sets.immutable.of("toggle1", "toggle2"), packageMetadata.getMetadataSection().getToggles());

        assertEquals(Maps.mutable.of(
                "a1", "UTF-8",
                "a2", "UTF-8",
                "a3", "UTF-8",
                "a4", "UTF-16"
                )
                , packageMetadata.getFileToEncodingMap());
    }

}