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
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.api.appdata.doc.TextMarkupDocument;
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.impl.DeployMetricsCollector;
import com.gs.obevo.impl.DeployMetricsCollectorImpl;
import com.gs.obevo.util.Tokenizer;
import com.gs.obevo.util.VisibleForTesting;
import com.gs.obevo.util.hash.DbChangeHashStrategy;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.collections.impl.utility.StringIterate.trimmedTokensToList;

public class TableChangeParser extends AbstractDbChangeFileParser {
    private static final Logger LOG = LoggerFactory.getLogger(TableChangeParser.class);
    private static final String ATTR_APPLY_GRANTS = "applyGrants";
    private static final String ATTR_CHANGESET = "changeset";
    private static final String ATTR_PARALLEL_GROUP = "parallelGroup";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_BASELINED_CHANGES = "baselinedChanges";
    private static final String ATTR_COMMENT = "comment";
    private static final String ATTR_TEMPLATE_PARAMS = "templateParams";
    private static final String TOGGLE_FK = "FK";
    private static final String TOGGLE_TRIGGER = "TRIGGER";
    private static final String TOGGLE_DROP_TABLE = "DROP_TABLE";
    private static final String TOGGLE_INACTIVE = "INACTIVE";
    private static final String TOGGLE_INDEX = "INDEX";

    private final DbChangeHashStrategy contentHashStrategy;
    private final ChangeType fkChangeType;
    private final ChangeType triggerChangeType;

    @VisibleForTesting
    public TableChangeParser(DbChangeHashStrategy contentHashStrategy, ChangeType fkChangeType, ChangeType triggerChangeType) {
        this(contentHashStrategy, fkChangeType, triggerChangeType, false, new DeployMetricsCollectorImpl(), new TextMarkupDocumentReader(false));
    }

    public TableChangeParser(DbChangeHashStrategy contentHashStrategy, ChangeType fkChangeType, ChangeType triggerChangeType, boolean backwardsCompatibleMode, DeployMetricsCollector deployMetricsCollector, TextMarkupDocumentReader textMarkupDocumentReader) {
        super(backwardsCompatibleMode, deployMetricsCollector,
                Sets.immutable.with(
                        ATTR_APPLY_GRANTS,
                        ATTR_BASELINED_CHANGES,
                        ATTR_CHANGESET,
                        ATTR_NAME,
                        ATTR_COMMENT,
                        ATTR_PARALLEL_GROUP,
                        ATTR_TEMPLATE_PARAMS,
                        TextMarkupDocumentReader.ATTR_DEPENDENCIES,
                        TextMarkupDocumentReader.ATTR_EXCLUDE_DEPENDENCIES,
                        TextMarkupDocumentReader.ATTR_INCLUDE_DEPENDENCIES,
                        TextMarkupDocumentReader.TAG_PERM_SCHEME,
                        TextMarkupDocumentReader.EXCLUDE_ENVS,
                        TextMarkupDocumentReader.EXCLUDE_PLATFORMS,
                        TextMarkupDocumentReader.INCLUDE_ENVS,
                        TextMarkupDocumentReader.INCLUDE_PLATFORMS
                ),
                Sets.immutable.with(
                        TextMarkupDocumentReader.TOGGLE_DISABLE_QUOTED_IDENTIFIERS,
                        TOGGLE_INACTIVE,
                        TOGGLE_FK,
                        TOGGLE_TRIGGER,
                        TOGGLE_INDEX,
                        TOGGLE_DROP_TABLE
                ),
                Sets.immutable.with(TextMarkupDocumentReader.TAG_DROP_COMMAND),
                textMarkupDocumentReader
        );
        this.contentHashStrategy = contentHashStrategy;
        this.fkChangeType = fkChangeType;
        this.triggerChangeType = triggerChangeType;
    }

    @Override
    public ImmutableList<Change> value(final ChangeType tableChangeType, final FileObject file, final String schema, final TextMarkupDocumentSection packageMetadata) {
        try {
            LOG.debug("Attempting to read file {}", file);

            final TextMarkupDocument origDoc = readDocument(file, packageMetadata).getOne();

            final TextMarkupDocumentSection metadata = this.getOrCreateMetadataNode(origDoc);

            String templateParamAttr = metadata.getAttr(ATTR_TEMPLATE_PARAMS);

            // Handle a potential template object; this will return a dummy params list if this is not a template object
            final ImmutableList<ImmutableMap<String, String>> templateParamsList = convertToParamList(templateParamAttr);

            ImmutableList<Pair<String, ImmutableList<Change>>> fileToChangePairs = templateParamsList.collect(new Function<MapIterable<String, String>, Pair<String, ImmutableList<Change>>>() {
                @Override
                public Pair<String, ImmutableList<Change>> valueOf(MapIterable<String, String> templateParams) {

                    Tokenizer tokenizer = new Tokenizer(templateParams, "${", "}");

                    final String objectName = tokenizer.tokenizeString(file.getName().getBaseName().split("\\.")[0]);
                    final TextMarkupDocument doc = templateParams.notEmpty()
                            ? readDocument(file, tokenizer.tokenizeString(file.getStringContent()), packageMetadata).getOne()
                            : origDoc;  /// if no template params, then save some effort and don't bother re-reading the doc from the string

                    final ParseDbChange parseDbChange = new ParseDbChange(contentHashStrategy, tableChangeType);

                    final ImmutableList<ArtifactRestrictions> fileLevelRestrictions = new DbChangeRestrictionsReader().valueOf(metadata);
                    ImmutableList<Change> changes = doc.getSections()
                            .select(Predicates.attributeEqual(TextMarkupDocumentSection.TO_NAME, TextMarkupDocumentReader.TAG_CHANGE))
                            .collect(new Function<TextMarkupDocumentSection, Change>() {
                                private int i = 0;

                                @Override
                                public Change valueOf(TextMarkupDocumentSection section) {

                                    ChangeIncremental change = parseDbChange.value(section, schema, objectName, this.i++);
                                    ImmutableList<ArtifactRestrictions> changeLevelRestrictions = new DbChangeRestrictionsReader().valueOf(section);
                                    change.setRestrictions(mergeRestrictions(fileLevelRestrictions, changeLevelRestrictions));
                                    change.setPermissionScheme(getPermissionSchemeValue(doc));
                                    change.setFileLocation(file);
                                    change.setMetadataSection(metadata);

                                    String dependenciesStr = section.getAttr(TextMarkupDocumentReader.ATTR_DEPENDENCIES);
                                    if (dependenciesStr != null) {
                                        change.setDependencies(Sets.immutable.with(dependenciesStr.split(",")).reject(StringPredicates.empty()));
                                    }

                                    String excludeDependenciesStr = section.getAttr(TextMarkupDocumentReader.ATTR_EXCLUDE_DEPENDENCIES);
                                    if (excludeDependenciesStr != null) {
                                        change.setExcludeDependencies(Sets.immutable.with(excludeDependenciesStr.split(",")).reject(StringPredicates.empty()));
                                    }
                                    return change;
                                }
                            });

                    return Tuples.pair(objectName, changes);
                }
            });

            // Validate that if we had used templates, that it resulted in different file names
            MutableSet<String> detemplatedObjectNames = fileToChangePairs.collect(Functions.<String>firstOfPair(), Sets.mutable.<String>empty());

            if (detemplatedObjectNames.size() != templateParamsList.size()) {
                throw new IllegalArgumentException("Expecting the usage of templates to result in a different file name per template set; expected " + templateParamsList.size() + " object names (from " + templateParamAttr + ") but found " + detemplatedObjectNames);
            }

            return fileToChangePairs.flatCollect(Functions.<ImmutableList<Change>>secondOfPair());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Error while parsing file " + file + " of change type " + tableChangeType.getName() + "; please see the cause in the stack trace below: " + e.getMessage(), e);
        }
    }

    private ImmutableList<ImmutableMap<String, String>> convertToParamList(String templateParamAttr) {
        if (templateParamAttr == null) {
            return Lists.immutable.of(Maps.immutable.<String, String>empty());
        }

        ImmutableList<String> paramGroups = ArrayAdapter.adapt(templateParamAttr.split(";")).toImmutable();
        return paramGroups.collect(new Function<String, ImmutableMap<String, String>>() {
            @Override
            public ImmutableMap<String, String> valueOf(String paramGroup) {
                String[] paramStrs = paramGroup.split(",");
                MutableMap<String, String> params = Maps.mutable.empty();
                for (String paramStr : paramStrs) {
                    String[] paramParts = paramStr.split("=");
                    params.put(paramParts[0], paramParts[1]);
                }

                return params.toImmutable();
            }
        });
    }

    @Override
    protected void validateStructureNew(TextMarkupDocument doc) {
        ImmutableList<TextMarkupDocumentSection> docSections = doc.getSections();

        if (docSections.isEmpty() || docSections.noneSatisfy(Predicates.attributeEqual(TextMarkupDocumentSection.TO_NAME, TextMarkupDocumentReader.TAG_CHANGE))) {
            throw new IllegalArgumentException("No //// " + TextMarkupDocumentReader.TAG_CHANGE + " sections found; at least one is required");
        }

        if (!(TextMarkupDocumentReader.TAG_CHANGE.equals(docSections.get(0).getName()) || TextMarkupDocumentReader.TAG_METADATA.equals(docSections.get(0).getName()))) {
            throw new IllegalArgumentException("First content of the file must be the //// CHANGE line, " +
                    "or a //// " + TextMarkupDocumentReader.TAG_METADATA + " line, " +
                    "or just a blank line");
        }

        if (TextMarkupDocumentReader.TAG_METADATA.equals(docSections.get(0).getName())) {
            docSections = docSections.subList(1, docSections.size());
        }

        ImmutableList<TextMarkupDocumentSection> badSections = docSections.reject(Predicates.attributeEqual(TextMarkupDocumentSection.TO_NAME, TextMarkupDocumentReader.TAG_CHANGE));
        if (badSections.notEmpty()) {
            throw new IllegalArgumentException("File structure for incremental file must be optionally a //// " + TextMarkupDocumentReader.TAG_METADATA + " section followed only by //// " + TextMarkupDocumentReader.TAG_CHANGE + " sections. Instead, found this section in between: " + badSections);
        }
    }

    @Override
    protected void validateStructureOld(TextMarkupDocument doc) {
        boolean contentEmpty = StringUtils.isBlank(doc.getSections().get(0).getContent());

        Validate.isTrue(doc.getSections().get(0).getName() != null || contentEmpty,
                "First content of the file must be the //// CHANGE line, or a //// "
                        + TextMarkupDocumentReader.TAG_METADATA + " line, or just a blank");

    }

    /**
     * Merge file and change level restrictions by restriction class name
     */
    private ImmutableList<ArtifactRestrictions> mergeRestrictions(ImmutableList<ArtifactRestrictions> fileLevelRestrictions, final ImmutableList<ArtifactRestrictions> changeLevelRestrictions) {
        return Lists.mutable.ofAll(fileLevelRestrictions)
                .withAll(changeLevelRestrictions)
                .groupBy(Functions.getToClass())
                .multiValuesView()
                .collect(new Function<RichIterable<ArtifactRestrictions>, ArtifactRestrictions>() {
                    @Override
                    public ArtifactRestrictions valueOf(RichIterable<ArtifactRestrictions> restictions) {
                        return restictions.getLast();
                    }
                })
                .toList().toImmutable();
    }

    class ParseDbChange {
        private final DbChangeHashStrategy contentHashStrategy;
        private final ChangeType tableChangeType;

        public ParseDbChange(DbChangeHashStrategy contentHashStrategy, ChangeType tableChangeType) {
            this.contentHashStrategy = contentHashStrategy;
            this.tableChangeType = tableChangeType;
        }

        private ChangeType getChangeType(TextMarkupDocumentSection section) {
            if (section.isTogglePresent(TOGGLE_FK)) {
                return fkChangeType;
            } else {
                if (section.isTogglePresent(TOGGLE_TRIGGER)) {
                    return triggerChangeType;
                } else {
                    return tableChangeType;
                }
            }
        }

        public ChangeIncremental value(TextMarkupDocumentSection section, String schema, String objectName,
                                       int changeIndex) {
            ChangeType changeType = getChangeType(section);
            boolean drop = section.isTogglePresent(TOGGLE_DROP_TABLE);

            String changeName = section.getAttr(ATTR_NAME);
            Validate.notNull(changeName, "Must define name parameter");

            MutableList<String> baselinedChanges = section.getAttr(ATTR_BASELINED_CHANGES) == null ? FastList.<String>newList() : trimmedTokensToList(
                    section.getAttr(ATTR_BASELINED_CHANGES), ",");

            boolean active = !section.isTogglePresent(TOGGLE_INACTIVE);

            TextMarkupDocumentSection rollbackIfAlreadyDeployedSection = section.getSubsections().detect(
                    Predicates.attributeEqual(TextMarkupDocumentSection.TO_NAME,
                            TextMarkupDocumentReader.TAG_ROLLBACK_IF_ALREADY_DEPLOYED));
            // in case the section exists but no content, mark it as an empty string so that changes can still get dropped from the audit log
            String rollbackIfAlreadyDeployedCommand = rollbackIfAlreadyDeployedSection == null ? null : StringUtils.defaultIfEmpty(rollbackIfAlreadyDeployedSection.getContent(), "");

            TextMarkupDocumentSection rollbackSection = section.getSubsections()
                    .detect(Predicates.attributeEqual(TextMarkupDocumentSection.TO_NAME,
                            TextMarkupDocumentReader.TAG_ROLLBACK));
            String rollbackContent = rollbackSection == null ? null : rollbackSection.getContent();

            String content = section.getContent() == null ? "" : section.getContent();
            if (StringUtils.isEmpty(content)) {
                LOG.warn("WARNING: Table {} has change name {} defined without any content in it; please " +
                                "double-check if this was intentional (ideally, we should not have empty changes)",
                        objectName, changeName);
            }
            ChangeIncremental change = new ChangeIncremental(changeType, schema, objectName, changeName,
                    changeIndex, this.contentHashStrategy.hashContent(content), content,
                    rollbackIfAlreadyDeployedCommand, active);
            change.setRollbackContent(rollbackContent);
            change.setBaselinedChanges(baselinedChanges);
            change.setDrop(drop);
            change.setKeepIncrementalOrder(drop);

            String applyGrantsStr = section.getAttr(ATTR_APPLY_GRANTS);
            change.setApplyGrants(applyGrantsStr == null ? null : Boolean.valueOf(applyGrantsStr));
            change.setChangeset(section.getAttr(ATTR_CHANGESET));
            change.setParallelGroup(section.getAttr(ATTR_PARALLEL_GROUP));
            return change;
        }
    }
}
