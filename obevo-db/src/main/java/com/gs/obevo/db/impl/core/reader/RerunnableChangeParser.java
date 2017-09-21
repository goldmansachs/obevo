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

import com.gs.obevo.api.appdata.ArtifactRestrictions;
import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeRerunnable;
import com.gs.obevo.api.appdata.doc.TextMarkupDocument;
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.impl.DeployMetricsCollector;
import com.gs.obevo.impl.DeployMetricsCollectorImpl;
import com.gs.obevo.util.VisibleForTesting;
import com.gs.obevo.util.hash.OldWhitespaceAgnosticDbChangeHashStrategy;
import com.gs.obevo.util.vfs.FileObject;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.block.factory.primitive.IntPredicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RerunnableChangeParser extends AbstractDbChangeFileParser {
    private static final Logger LOG = LoggerFactory.getLogger(RerunnableChangeParser.class);
    private static final String ATTR_ORDER = "order";
    private final OldWhitespaceAgnosticDbChangeHashStrategy contentHashStrategy = new OldWhitespaceAgnosticDbChangeHashStrategy();

    @VisibleForTesting
    public RerunnableChangeParser() {
        this(false, new DeployMetricsCollectorImpl(), new TextMarkupDocumentReader(false));
    }

    public RerunnableChangeParser(boolean backwardsCompatibleMode, DeployMetricsCollector deployMetricsCollector, TextMarkupDocumentReader textMarkupDocumentReader) {
        super(backwardsCompatibleMode, deployMetricsCollector,
                Sets.immutable.with(
                        TextMarkupDocumentReader.ATTR_DEPENDENCIES,
                        TextMarkupDocumentReader.ATTR_EXCLUDE_DEPENDENCIES,
                        TextMarkupDocumentReader.ATTR_INCLUDE_DEPENDENCIES,
                        TextMarkupDocumentReader.TAG_PERM_SCHEME,
                        TextMarkupDocumentReader.EXCLUDE_ENVS,
                        TextMarkupDocumentReader.EXCLUDE_PLATFORMS,
                        TextMarkupDocumentReader.INCLUDE_ENVS,
                        TextMarkupDocumentReader.INCLUDE_PLATFORMS,
                        TextMarkupDocumentReader.ATTR_UPDATE_TIME_COLUMN,
                        TextMarkupDocumentReader.ATTR_PRIMARY_KEYS,
                        ATTR_ORDER
                ),
                Sets.immutable.with(
                        TextMarkupDocumentReader.TOGGLE_DISABLE_QUOTED_IDENTIFIERS
                ),
                Sets.immutable.with(TextMarkupDocumentReader.TAG_CHANGE),
                textMarkupDocumentReader
        );
    }

    @Override
    public ImmutableList<Change> value(ChangeType changeType, FileObject file, String fileContent, String objectName, String schema, TextMarkupDocumentSection packageMetadata) {
        ObjectBooleanPair<TextMarkupDocument> docStatusPair = readDocument(fileContent, packageMetadata);
        TextMarkupDocument doc = docStatusPair.getOne();
        boolean backwardsCompatibleModeUsed = docStatusPair.getTwo();

        TextMarkupDocumentSection mainSection = getMainSection(doc, backwardsCompatibleModeUsed);
        String contentToUse = mainSection.getContent();

        ChangeRerunnable change = new ChangeRerunnable(changeType
                , schema
                , objectName
                , contentHashStrategy.hashContent(contentToUse)
                , contentToUse);

        TextMarkupDocumentSection metadata = getOrCreateMetadataNode(doc);

        ImmutableList<ArtifactRestrictions> restrictions = new DbChangeRestrictionsReader().valueOf(metadata);
        change.setRestrictions(restrictions);

        String dependenciesStr = metadata.getAttr(TextMarkupDocumentReader.ATTR_DEPENDENCIES);
        String excludeDependenciesStr = metadata.getAttr(TextMarkupDocumentReader.ATTR_EXCLUDE_DEPENDENCIES);
        String includeDependenciesStr = metadata.getAttr(TextMarkupDocumentReader.ATTR_INCLUDE_DEPENDENCIES);

        if (dependenciesStr != null && (excludeDependenciesStr != null || includeDependenciesStr != null)) {
            throw new IllegalArgumentException(String.format("Cannot specify the %1%s attribute with either the %2$s or %3$s attributes; either go w/ just %1$s or a combo of %2$s and %3$s or neither",
                    TextMarkupDocumentReader.ATTR_DEPENDENCIES,
                    TextMarkupDocumentReader.ATTR_EXCLUDE_DEPENDENCIES,
                    TextMarkupDocumentReader.ATTR_INCLUDE_DEPENDENCIES));
        }

        if (dependenciesStr != null) {
            change.setDependencies(Sets.immutable.with(dependenciesStr.split(",")).reject(StringPredicates.empty()));
        } else {
            if (excludeDependenciesStr != null) {
                change.setExcludeDependencies(Sets.immutable.with(excludeDependenciesStr.split(",")).reject(StringPredicates.empty()));
            }
            if (includeDependenciesStr != null) {
                change.setIncludeDependencies(Sets.immutable.with(includeDependenciesStr.split(",")).reject(StringPredicates.empty()));
            }
        }

        String orderStr = metadata.getAttr(ATTR_ORDER);
        if (orderStr != null) {
            change.setOrder(Integer.valueOf(orderStr));
        }

        change.setPermissionScheme(getPermissionSchemeValue(doc));
        change.setMetadataSection(doc.findSectionWithElementName(TextMarkupDocumentReader.TAG_METADATA));

        TextMarkupDocumentSection dropCommandSection = doc.findSectionWithElementName(TextMarkupDocumentReader.TAG_DROP_COMMAND);
        change.setDropContent(dropCommandSection != null ? dropCommandSection.getContent() : null);

        change.setFileLocation(file);
        return Lists.immutable.<Change>with(change);
    }

    @Override
    protected void validateStructureOld(TextMarkupDocument doc) {
        final MutableSet<String> foundSections = doc.getSections().collect(TextMarkupDocumentSection.TO_NAME).toSet().reject(Predicates.isNull());
        final MutableSet<String> expectedSections = Sets.mutable.with(TextMarkupDocumentReader.TAG_METADATA, TextMarkupDocumentReader.TAG_DROP_COMMAND);
        final MutableSet<String> extraSections = foundSections.difference(expectedSections);
        if (extraSections.notEmpty()) {
            throw new IllegalArgumentException("Unexpected sections found: " + extraSections + "; only expecting: " + expectedSections);
        }
    }

    @Override
    protected void validateStructureNew(TextMarkupDocument doc) {
        String allowedSectionString = Sets.immutable.with(TextMarkupDocumentReader.TAG_METADATA, TextMarkupDocumentReader.TAG_DROP_COMMAND).collect(StringFunctions.prepend("//// ")).makeString(", ");

        ImmutableList<TextMarkupDocumentSection> docSections = doc.getSections();
        if (docSections.isEmpty()) {
            throw new IllegalArgumentException("No content defined");
        }

        ImmutableList<TextMarkupDocumentSection> disallowedSections = docSections.reject(Predicates.attributeIn(TextMarkupDocumentSection.TO_NAME, Sets.immutable.with(null, TextMarkupDocumentReader.TAG_METADATA, TextMarkupDocumentReader.TAG_DROP_COMMAND)));
        if (disallowedSections.notEmpty()) {
            throw new IllegalArgumentException("Only allowed 1 content section and at most 1 of these [" + allowedSectionString + "]; instead, found these disallowed sections: " + disallowedSections);
        }

        ImmutableList<String> sectionNames = docSections.collect(TextMarkupDocumentSection.TO_NAME);
        MutableBag<String> duplicateSections = sectionNames.toBag().selectByOccurrences(IntPredicates.greaterThan(1));
        if (duplicateSections.notEmpty()) {
            throw new IllegalArgumentException("Only allowed 1 content section and at most 1 of these [" + allowedSectionString + "]; instead, found these extra sections instances: " + duplicateSections.toSet());
        }

        int metadataIndex = sectionNames.indexOf(TextMarkupDocumentReader.TAG_METADATA);
        int contentIndex = sectionNames.indexOf(null);
        int dropIndexIndex = sectionNames.indexOf(TextMarkupDocumentReader.TAG_DROP_COMMAND);

        if (metadataIndex != -1 && contentIndex != -1 && metadataIndex > contentIndex) {
            throw new IllegalArgumentException("Improper section ordering: " + TextMarkupDocumentReader.TAG_METADATA + " section must come before the content section");
        } else if (contentIndex != -1 && dropIndexIndex != -1 && contentIndex > dropIndexIndex) {
            throw new IllegalArgumentException("Improper section ordering: content section must come before the " + TextMarkupDocumentReader.TAG_DROP_COMMAND + " section");
        }

    }

    private TextMarkupDocumentSection getMainSection(TextMarkupDocument doc, boolean backwardsCompatibleModeUsed) {
        return backwardsCompatibleModeUsed ? getMainSectionOld(doc) : getMainSectionNew(doc);
    }

    private TextMarkupDocumentSection getMainSectionNew(TextMarkupDocument doc) {
        return doc.findSectionWithElementName(null);
    }

    private TextMarkupDocumentSection getMainSectionOld(TextMarkupDocument doc) {
        return doc.getSections().get(0);
    }
}
