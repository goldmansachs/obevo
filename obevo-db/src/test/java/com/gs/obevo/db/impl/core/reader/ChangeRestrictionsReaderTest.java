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

import com.gs.obevo.api.appdata.ArtifactEnvironmentRestrictions;
import com.gs.obevo.api.appdata.ArtifactPlatformRestrictions;
import com.gs.obevo.api.appdata.ArtifactRestrictions;
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ChangeRestrictionsReaderTest {
    private final DbChangeRestrictionsReader restrictionsReader = new DbChangeRestrictionsReader();

    private TextMarkupDocumentSection doc(String sectionName, String sectionContent, ImmutableMap<String, String> attrs) {
        return new TextMarkupDocumentSection(sectionName, sectionContent, attrs);
    }

    @Test
    public void testNothing() {
        ImmutableList<ArtifactRestrictions> restrictions = this.restrictionsReader.valueOf(this.doc(null, "regularContent", null));
        assertTrue(restrictions.isEmpty());
    }

    @Test
    public void testIncludeEnvs() {
        ImmutableList<ArtifactRestrictions> restrictions = this.restrictionsReader.valueOf(
                this.doc(TextMarkupDocumentReader.TAG_METADATA, null,
                        Maps.immutable.with("includeEnvs", "dev1,dev3"))
        );
        assertEquals(1, restrictions.size());
        assertThat(restrictions.getFirst(), instanceOf(ArtifactEnvironmentRestrictions.class));
        assertEquals(UnifiedSet.newSetWith("dev1", "dev3"), restrictions.getFirst().getIncludes());
        assertTrue(restrictions.getFirst().getExcludes().isEmpty());
    }

    @Test
    public void testExcludeEnvs() {
        ImmutableList<ArtifactRestrictions> restrictions = this.restrictionsReader.valueOf(
                this.doc(TextMarkupDocumentReader.TAG_METADATA, null,
                        Maps.immutable.with("excludeEnvs", "dev1,dev3"))
        );
        assertEquals(1, restrictions.size());
        assertThat(restrictions.getFirst(), instanceOf(ArtifactEnvironmentRestrictions.class));
        assertEquals(UnifiedSet.newSetWith("dev1", "dev3"), restrictions.getFirst().getExcludes());
        assertTrue(restrictions.getFirst().getIncludes().isEmpty());
    }

    @Test
    public void testWrongNoEnvsSpecified() {
        assertTrue(this.restrictionsReader.valueOf(
                this.doc(TextMarkupDocumentReader.TAG_METADATA, null,
                        Maps.immutable.with("wrongIncludePrefix", "dev1,dev3"))
        ).isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongBothIncludeExcludeEnvsSpecified() {
        this.restrictionsReader.valueOf(
                this.doc(TextMarkupDocumentReader.TAG_METADATA, null,
                        Maps.immutable.with("includeEnvs", "dev1,dev3", "excludeEnvs", "dev1,dev3"))
        );
    }


    @Test
    public void testIncludePlatforms() {
        ImmutableList<ArtifactRestrictions> restrictions = this.restrictionsReader.valueOf(
                this.doc(TextMarkupDocumentReader.TAG_METADATA, null,
                        Maps.immutable.with("includePlatforms", "DB2,SYBASE_ASE"))
        );
        assertEquals(1, restrictions.size());
        assertThat(restrictions.getFirst(), instanceOf(ArtifactPlatformRestrictions.class));
        assertEquals(UnifiedSet.newSetWith("DB2", "SYBASE_ASE"), restrictions.getFirst().getIncludes());
        assertTrue(restrictions.getFirst().getExcludes().isEmpty());
    }

    @Test
    public void testExcludePlatforms() {
        ImmutableList<ArtifactRestrictions> restrictions = this.restrictionsReader.valueOf(
                this.doc(TextMarkupDocumentReader.TAG_METADATA, null,
                        Maps.immutable.with("excludePlatforms", "HSQL,HIVE"))
        );
        assertEquals(1, restrictions.size());
        assertEquals(UnifiedSet.newSetWith("HSQL", "HIVE"), restrictions.getFirst().getExcludes());
        assertTrue(restrictions.getFirst().getIncludes().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongBothIncludeExcludePlatformsSpecified() {
        this.restrictionsReader.valueOf(
                this.doc(TextMarkupDocumentReader.TAG_METADATA, null,
                        Maps.immutable.with("includePlatforms", "DB2,SYBASE_ASE", "excludePlatforms", "HSQL,HIVE"))
        );
    }

    @Test
    public void testIncludeEnvsExcludePlatforms() {
        ImmutableList<ArtifactRestrictions> restrictions = this.restrictionsReader.valueOf(
                this.doc(TextMarkupDocumentReader.TAG_METADATA, null,
                        Maps.immutable.with(
                                "includeEnvs", "dev1,dev3",
                                "excludePlatforms", "HSQL,HIVE"))
        );
        assertEquals(2, restrictions.size());

        assertThat(restrictions.getFirst(), instanceOf(ArtifactEnvironmentRestrictions.class));
        assertEquals(UnifiedSet.newSetWith("dev1", "dev3"), restrictions.getFirst().getIncludes());
        assertTrue(restrictions.getFirst().getExcludes().isEmpty());

        assertThat(restrictions.getLast(), instanceOf(ArtifactPlatformRestrictions.class));
        assertTrue(restrictions.getLast().getIncludes().isEmpty());
        assertEquals(UnifiedSet.newSetWith("HSQL", "HIVE"), restrictions.getLast().getExcludes());
    }


}
