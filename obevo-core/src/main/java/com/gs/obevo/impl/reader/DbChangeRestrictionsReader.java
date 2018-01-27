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

import com.gs.obevo.api.appdata.ArtifactEnvironmentRestrictions;
import com.gs.obevo.api.appdata.ArtifactPlatformRestrictions;
import com.gs.obevo.api.appdata.ArtifactRestrictions;
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

class DbChangeRestrictionsReader {
    public ImmutableList<ArtifactRestrictions> valueOf(TextMarkupDocumentSection section) {
        if (section == null) {
            return Lists.immutable.of();
        }

        MutableList<ArtifactRestrictions> restrictions = Lists.mutable.empty();

        Twin<MutableSet<String>> envRestrictions = readRestrictions(section, TextMarkupDocumentReader.INCLUDE_ENVS, TextMarkupDocumentReader.EXCLUDE_ENVS);
        if (envRestrictions != null) {
            restrictions.add(new ArtifactEnvironmentRestrictions(envRestrictions.getOne(), envRestrictions.getTwo()));
        }

        Twin<MutableSet<String>> platformRestrictions = readRestrictions(section, TextMarkupDocumentReader.INCLUDE_PLATFORMS, TextMarkupDocumentReader.EXCLUDE_PLATFORMS);
        if (platformRestrictions != null) {
            restrictions.add(new ArtifactPlatformRestrictions(platformRestrictions.getOne(), platformRestrictions.getTwo()));
        }

        return restrictions.toImmutable();
    }

    private Twin<MutableSet<String>> readRestrictions(TextMarkupDocumentSection section, String includeKey, String excludeKey) {
        MutableSet<String> include = readList(section, includeKey);
        MutableSet<String> exclude = readList(section, excludeKey);
        if (include != null && exclude != null) {
            throw new IllegalArgumentException(
                    String.format("Cannot define the %s param with both %s and %s; must be one or the other", TextMarkupDocumentReader.TAG_METADATA, includeKey, excludeKey)
            );
        } else if (include == null && exclude == null) {
            return null;
        } else {
            return Tuples.twin(include, exclude);
        }
    }

    private MutableSet<String> readList(TextMarkupDocumentSection section, String key) {
        String val = section.getAttr(key);
        if (val == null) {
            return null;
        } else {
            return UnifiedSet.newSetWith(val.split(","));
        }
    }
}
