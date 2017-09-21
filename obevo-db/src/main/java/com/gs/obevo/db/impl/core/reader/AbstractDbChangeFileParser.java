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
import com.gs.obevo.api.platform.DeployMetrics;
import com.gs.obevo.impl.DeployMetricsCollector;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * Common baseline for the DbChangeFileParsers with common implementation methods.
 */
public abstract class AbstractDbChangeFileParser implements DbChangeFileParser {
    private final ImmutableSet<String> disallowedSectionNames;
    private final ImmutableSet<String> allowedAttrs;
    private final ImmutableSet<String> allowedToggles;
    private final boolean backwardsCompatibleMode;
    private final DeployMetricsCollector deployMetricsCollector;
    private final TextMarkupDocumentReader textMarkupDocumentReader;

    protected AbstractDbChangeFileParser(boolean backwardsCompatibleMode, DeployMetricsCollector deployMetricsCollector, ImmutableSet<String> allowedAttrs, ImmutableSet<String> allowedToggles, ImmutableSet<String> disallowedSectionNames, TextMarkupDocumentReader textMarkupDocumentReader) {
        this.disallowedSectionNames = disallowedSectionNames;
        this.allowedAttrs = allowedAttrs;
        this.allowedToggles = allowedToggles;
        this.backwardsCompatibleMode = backwardsCompatibleMode;
        this.deployMetricsCollector = deployMetricsCollector;
        this.textMarkupDocumentReader = textMarkupDocumentReader;
    }

    protected String getPermissionSchemeValue(TextMarkupDocument doc) {
        TextMarkupDocumentSection section = doc.findSectionWithElementName(TextMarkupDocumentReader.TAG_METADATA);
        if (section != null) {
            String permScheme = section.getAttr(TextMarkupDocumentReader.TAG_PERM_SCHEME);
            if (permScheme != null) {
                return permScheme;
            }
        }
        return null;
    }

    protected final ObjectBooleanPair<TextMarkupDocument> readDocument(String fileContent, TextMarkupDocumentSection packageMetadata) {
        try {
            TextMarkupDocument doc = textMarkupDocumentReader.parseString(fileContent, packageMetadata);
            validateStructureNew(doc);
            validateAttributes(doc);
            return PrimitiveTuples.pair(doc, false);
        } catch (RuntimeException newExc) {
            if (backwardsCompatibleMode) {
                try {
                    TextMarkupDocument doc = new TextMarkupDocumentReaderOld().parseString(fileContent, packageMetadata);
                    validateStructureOld(doc);
                    deployMetricsCollector.addListMetric(DeployMetrics.BAD_FILE_FORMAT_WARNINGS, newExc);
                    return PrimitiveTuples.pair(doc, true);
                } catch (RuntimeException oldExc) {
                    throw new RuntimeException("Failed to parse the file in both the old and new formats:\nOld: " + oldExc.getMessage() + "\nNew: " + newExc.getMessage() + "\nStack Trace: ", newExc);
                }
            } else {
                throw newExc;
            }
        }
    }

    protected abstract void validateStructureOld(TextMarkupDocument doc);

    protected abstract void validateStructureNew(TextMarkupDocument doc);

    private void validateAttributes(TextMarkupDocument doc) {
        ImmutableList<String> disallowedSections = doc.getSections().collect(TextMarkupDocumentSection.TO_NAME).select(Predicates.in(disallowedSectionNames));

        if (disallowedSections.notEmpty()) {
            throw new IllegalArgumentException("Found these disallowed sections: " + disallowedSections);
        }

        for (TextMarkupDocumentSection section : doc.getSections()) {
            ImmutableSet<String> disallowedAttrs = section.getAttrs().keysView().toSet().toImmutable().difference(allowedAttrs);
            ImmutableSet<String> disallowedToggles = section.getToggles().difference(allowedToggles);

            MutableList<String> errorMessages = Lists.mutable.empty();
            if (disallowedAttrs.notEmpty()) {
                errorMessages.add("Following attributes are not allowed in the " + section.getName() + " section: " + disallowedAttrs);
            }
            if (disallowedToggles.notEmpty()) {
                errorMessages.add("Following toggles are not allowed in the " + section.getName() + " section: " + disallowedToggles);
            }
            if (errorMessages.notEmpty()) {
                throw new IllegalArgumentException("Found " + errorMessages.size() + " errors in the input: " + errorMessages.makeString(";;; "));
            }
        }
    }

    protected TextMarkupDocumentSection getOrCreateMetadataNode(TextMarkupDocument doc) {
        TextMarkupDocumentSection metadata = doc.findSectionWithElementName(TextMarkupDocumentReader.TAG_METADATA);
        if (metadata == null) {
            metadata = new TextMarkupDocumentSection(TextMarkupDocumentReader.TAG_METADATA, null);
        }
        return metadata;
    }
}
