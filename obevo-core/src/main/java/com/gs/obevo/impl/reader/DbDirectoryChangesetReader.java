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

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployMetrics;
import com.gs.obevo.api.platform.FileSourceContext;
import com.gs.obevo.api.platform.FileSourceParams;
import com.gs.obevo.impl.DeployMetricsCollector;
import com.gs.obevo.impl.DeployMetricsCollectorImpl;
import com.gs.obevo.impl.OnboardingStrategy;
import com.gs.obevo.impl.reader.TableChangeParser.GetChangeType;
import com.gs.obevo.util.VisibleForTesting;
import com.gs.obevo.util.hash.OldWhitespaceAgnosticDbChangeHashStrategy;
import com.gs.obevo.util.vfs.BasicFileSelector;
import com.gs.obevo.util.vfs.CharsetStrategy;
import com.gs.obevo.util.vfs.CharsetStrategyFactory;
import com.gs.obevo.util.vfs.FileObject;
import com.gs.obevo.util.vfs.NotFileFilter;
import com.gs.obevo.util.vfs.TrueFileFilter;
import com.gs.obevo.util.vfs.WildcardFileFilter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.vfs2.FileFilter;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.eclipse.collections.api.partition.list.PartitionImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.gs.obevo.util.vfs.FileFilterUtils.and;
import static com.gs.obevo.util.vfs.FileFilterUtils.directory;
import static com.gs.obevo.util.vfs.FileFilterUtils.not;
import static com.gs.obevo.util.vfs.FileFilterUtils.or;
import static com.gs.obevo.util.vfs.FileFilterUtils.vcsAware;

public class DbDirectoryChangesetReader implements FileSourceContext {
    private static final Logger LOG = LoggerFactory.getLogger(DbDirectoryChangesetReader.class);

    /*
     * package-private for unit tests
     */
    static final FileFilter CHANGES_WILDCARD_FILTER = new WildcardFileFilter("*.changes.*");

    private final Function<String, String> convertDbObjectName;
    private final PackageMetadataReader packageMetadataReader;
    private final ConcurrentMutableMap<FileObject, PackageMetadata> packageMetadataCache = new ConcurrentHashMap<>();
    private final DbChangeFileParser tableChangeParser;
    private final DbChangeFileParser baselineTableChangeParser;
    private final DbChangeFileParser rerunnableChangeParser;
    private final DeployMetricsCollector deployMetricsCollector;

    public DbDirectoryChangesetReader(Function<String, String> convertDbObjectName, Environment env, DeployMetricsCollector deployMetricsCollector, boolean backwardsCompatibleMode, TextMarkupDocumentReader textMarkupDocumentReader, DbChangeFileParser baselineTableChangeParser, GetChangeType getChangeType) {
        this.packageMetadataReader = new PackageMetadataReader(textMarkupDocumentReader);
        this.convertDbObjectName = convertDbObjectName;
        this.tableChangeParser = new TableChangeParser(new OldWhitespaceAgnosticDbChangeHashStrategy(), backwardsCompatibleMode, deployMetricsCollector, textMarkupDocumentReader, getChangeType);
        this.baselineTableChangeParser = baselineTableChangeParser;
        this.rerunnableChangeParser = new RerunnableChangeParser(backwardsCompatibleMode, deployMetricsCollector, textMarkupDocumentReader);
        this.deployMetricsCollector = deployMetricsCollector;
    }

    @VisibleForTesting
    DbDirectoryChangesetReader(Function<String, String> convertDbObjectName, DbChangeFileParser tableChangeParser,
            DbChangeFileParser baselineTableChangeParser, DbChangeFileParser rerunnableChangeParser) {
        this.packageMetadataReader = new PackageMetadataReader(new TextMarkupDocumentReader(false));
        this.convertDbObjectName = convertDbObjectName;
        this.tableChangeParser = tableChangeParser;
        this.baselineTableChangeParser = baselineTableChangeParser;
        this.rerunnableChangeParser = rerunnableChangeParser;
        this.deployMetricsCollector = new DeployMetricsCollectorImpl();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.gs.obevo.db.newdb.DbChangeReader#readChanges(java.io.File)
     */
    @Override
    public ImmutableList<Change> readChanges(FileSourceParams fileSourceParams) {
        if (fileSourceParams.isBaseline() && baselineTableChangeParser == null) {
            throw new IllegalArgumentException("Cannot invoke readChanges with useBaseline == true if baselineTableChangeParser hasn't been set; baseline reading may not be enabled in your Platform type");
        }

        MutableList<Change> allChanges = Lists.mutable.empty();

        SetIterable<String> envSchemas = fileSourceParams.getSchemaNames().collect(this.convertDbObjectName);

        Validate.notEmpty(envSchemas.toSet(), "Environment must have schemas populated");

        for (FileObject sourceDir : fileSourceParams.getFiles()) {
            for (FileObject schemaDir : sourceDir.findFiles(new BasicFileSelector(and(vcsAware(), directory()), false))) {
                String schema = schemaDir.getName().getBaseName();

                if (envSchemas.contains(this.convertDbObjectName.valueOf(schema))) {
                    MutableList<Change> schemaChanges = Lists.mutable.empty();

                    for (ChangeType changeType : fileSourceParams.getChangeTypes()) {
                        FileObject changeTypeDir = this.findDirectoryForChangeType(schemaDir, changeType, fileSourceParams.isLegacyDirectoryStructureEnabled());

                        if (changeTypeDir != null) {
                            ImmutableList<Change> changes;
                            if (changeType.isRerunnable()) {
                                changes = findChanges(changeType, changeTypeDir, this.rerunnableChangeParser, TrueFileFilter.INSTANCE, schema, fileSourceParams.getAcceptedExtensions(), fileSourceParams.getDefaultSourceEncoding());
                            } else {
                                changes = findTableChanges(changeType, changeTypeDir, schema, fileSourceParams.isBaseline(), fileSourceParams.getAcceptedExtensions(), fileSourceParams.getDefaultSourceEncoding());
                            }
                            schemaChanges.withAll(changes);
                        }
                    }

                    MutableCollection<Change> tableChanges = schemaChanges.select(
                            Predicates.attributeIn(Change.TO_CHANGE_TYPE_NAME, Sets.immutable.of(ChangeType.TABLE_STR, ChangeType.FOREIGN_KEY_STR)));
                    MutableMultimap<String, Change> tableChangeMap = tableChanges.groupBy(Change.TO_DB_OBJECT_KEY);

                    MutableCollection<Change> staticDataChanges = schemaChanges.select(Predicates.attributeEqual(Change.TO_CHANGE_TYPE_NAME, ChangeType.STATICDATA_STR));

                    // now enrich the staticData objects w/ the information from the tables to facilitate the
                    // deployment order calculation.
                    for (Change staticDataChange : staticDataChanges) {
                        MutableCollection<Change> relatedTableChanges = tableChangeMap.get(staticDataChange.getDbObjectKey());
                        MutableCollection<Change> foreignKeys = relatedTableChanges.select(
                                Predicates.attributeEqual(Change.TO_CHANGE_TYPE_NAME, ChangeType.FOREIGN_KEY_STR));

                        String fkContent = foreignKeys.collect(Change.TO_CONTENT).makeString("\n\n");

                        staticDataChange.setContentForDependencyCalculation(fkContent);
                    }

                    allChanges.addAll(schemaChanges);
                } else {
                    LOG.info("Skipping schema directory [{}] as it was not defined among the schemas in your system-config.xml file: {}", schema, envSchemas);
                    continue;
                }
            }
        }

        return allChanges.toImmutable();
    }

    /**
     * We have this here to look for directories in the various places we had specified it in the code before (i.e.
     * for backwards-compatibility).
     */
    private FileObject findDirectoryForChangeType(FileObject schemaDir, ChangeType changeType, boolean legacyDirectoryStructureEnabled) {
        FileObject dir = schemaDir.getChild(changeType.getDirectoryName());
        if (dir != null && dir.exists()) {
            return dir;
        }
        dir = schemaDir.getChild(changeType.getDirectoryName().toUpperCase());
        if (dir != null && dir.exists()) {
            return dir;
        }
        if (legacyDirectoryStructureEnabled && changeType.getDirectoryNameOld() != null) {
            // for backwards-compatibility
            dir = schemaDir.getChild(changeType.getDirectoryNameOld());
            if (dir != null && dir.exists()) {
                deployMetricsCollector.addMetric("oldDirectoryNameUsed", true);
                return dir;
            }
        }

        return null;
    }

    private ImmutableList<Change> findTableChanges(ChangeType changeType, FileObject tableDir, String schema, boolean useBaseline, ImmutableSet<String> acceptedExtensions, String sourceEncoding) {
        WildcardFileFilter baselineFilter = new WildcardFileFilter("*.baseline.*");

        ImmutableList<FileObject> nonBaselineFiles = findFiles(tableDir,
                this.isUsingChangesConvention(tableDir) ? CHANGES_WILDCARD_FILTER : new NotFileFilter(baselineFilter), acceptedExtensions);

        ImmutableList<Change> nonBaselineChanges = parseChanges(changeType, nonBaselineFiles, this.tableChangeParser, schema, sourceEncoding);
        ImmutableListMultimap<String, Change> nonBaselineChangeMap = nonBaselineChanges
                .groupBy(Change.TO_DB_OBJECT_KEY);

        if (useBaseline) {
            LOG.info("Using the 'useBaseline' mode to read in the db changes");
            ImmutableList<FileObject> baselineFiles = findFiles(tableDir,
                    this.isUsingChangesConvention(tableDir) ? CHANGES_WILDCARD_FILTER : baselineFilter, acceptedExtensions);
            ImmutableList<Change> baselineChanges = parseChanges(changeType, baselineFiles, this.baselineTableChangeParser, schema, sourceEncoding);

            for (Change baselineChange : baselineChanges) {
                ImmutableList<Change> regularChanges = nonBaselineChangeMap.get(baselineChange
                        .getDbObjectKey());
                if (regularChanges.isEmpty()) {
                    throw new IllegalArgumentException("Invalid state - expecting a change here for this object key: "
                            + baselineChange.getDbObjectKey());
                }
                Change regularChange = regularChanges.get(0);

                this.copyDbObjectMetadataOverToBaseline(regularChange, baselineChange);
            }

            MutableSet<String> baselineDbObjectKeys = baselineChanges.collect(Change.TO_DB_OBJECT_KEY)
                    .toSet();
            LOG.info("Found the following baseline changes: will try to deploy via baseline for these db objects: "
                    + baselineDbObjectKeys.makeString(","));

            nonBaselineChanges = nonBaselineChanges
                    .reject(Predicates.attributeIn(Change.TO_DB_OBJECT_KEY, baselineDbObjectKeys))
                    .newWithAll(baselineChanges);
        }

        return nonBaselineChanges;
    }

    /**
     * This is for stuff in the METADATA tag that would be in the changes file but not the baseline file
     */
    private void copyDbObjectMetadataOverToBaseline(Change regularChange, Change baselineChange) {
        baselineChange.setRestrictions(regularChange.getRestrictions());
        baselineChange.setPermissionScheme(regularChange.getPermissionScheme());
    }

    private ImmutableList<FileObject> findFiles(FileObject dir, FileFilter fileFilter, final ImmutableSet<String> acceptedExtensions) {
        BasicFileSelector positiveSelector = new BasicFileSelector(
                and(
                        fileFilter,
                        not(directory()),
                        not(
                                or(
                                        new WildcardFileFilter("package-info*"),
                                        new WildcardFileFilter("*." + OnboardingStrategy.EXCEPTION_EXTENSION)
                                )
                        )
                ),
                vcsAware()
        );

        ImmutableList<FileObject> candidateFiles = Lists.immutable.with(dir.findFiles(positiveSelector));
        PartitionImmutableList<FileObject> extensionPartition = candidateFiles.partition(new Predicate<FileObject>() {
            @Override
            public boolean accept(FileObject each) {
                return acceptedExtensions.contains(each.getName().getExtension().toLowerCase());
            }
        });

        if (extensionPartition.getRejected().notEmpty()) {
            String message = "Found unexpected file extensions (these will not be deployed!) in directory " + dir + "; expects extensions [" + acceptedExtensions + "], found: " + extensionPartition.getRejected();
            LOG.warn(message);
            deployMetricsCollector.addListMetric(DeployMetrics.UNEXPECTED_FILE_EXTENSIONS, message);
        }

        return extensionPartition.getSelected();
    }

    private ImmutableList<Change> parseChanges(final ChangeType changeType, ImmutableList<FileObject> files,
            final DbChangeFileParser changeParser, final String schema, final String sourceEncoding) {
        return files.flatCollect(new Function<FileObject, ImmutableList<Change>>() {
            @Override
            public ImmutableList<Change> valueOf(FileObject file) {
                PackageMetadata packageMetadata = getPackageMetadata(file, sourceEncoding);
                String encoding = null;
                TextMarkupDocumentSection metadataSection = null;
                if (packageMetadata != null) {
                    encoding = packageMetadata.getFileToEncodingMap().get(file.getName().getBaseName());
                    metadataSection = packageMetadata.getMetadataSection();
                }
                CharsetStrategy charsetStrategy = CharsetStrategyFactory.getCharsetStrategy(ObjectUtils.defaultIfNull(encoding, sourceEncoding));
                final String objectName = file.getName().getBaseName().split("\\.")[0];
                try {
                    LOG.debug("Attempting to read file {}", file);
                    return changeParser.value(changeType, file, file.getStringContent(charsetStrategy), objectName, schema, metadataSection);
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException("Error while parsing file " + file + " of change type " + changeType.getName() + "; please see the cause in the stack trace below: " + e.getMessage(), e);
                }
            }
        });
    }

    private PackageMetadata getPackageMetadata(final FileObject file, final String sourceEncoding) {
        return packageMetadataCache.getIfAbsentPut(file.getParent(), new Function0<PackageMetadata>() {
            @Override
            public PackageMetadata value() {
                FileObject packageMetadataFile = file.getParent().getChild("package-info.txt");

                // we check for containsKey, as we may end up persisting null as the value in the map
                if (packageMetadataFile == null || !packageMetadataFile.isReadable()) {
                    return null;
                } else {
                    return packageMetadataReader.getPackageMetadata(packageMetadataFile.getStringContent(CharsetStrategyFactory.getCharsetStrategy(sourceEncoding)));
                }
            }
        });
    }

    private ImmutableList<Change> findChanges(final ChangeType changeType, FileObject dir, final DbChangeFileParser changeParser,
            FileFilter fileFilter, final String schema, ImmutableSet<String> acceptedExtensions, String sourceEncoding) {
        return parseChanges(changeType, findFiles(dir, fileFilter, acceptedExtensions), changeParser, schema, sourceEncoding);
    }

    /**
     * We have this check here for backwards-compatibility
     * Originally, we required the db files to end in .changes.*, but we now want to have the convention of the stard
     * file name being the
     * file that you edit, much like the other ones
     *
     * Originally, we wanted to regular name to be for the baseline, but given this is not yet implemented and won't
     * drive the changes, this was a wrong idea from the start. Eventually, we will have the baseline files in
     * .baseline.*
     */
    private boolean isUsingChangesConvention(FileObject dir) {
        FileObject[] files = dir.findFiles(new BasicFileSelector(CHANGES_WILDCARD_FILTER, false));
        boolean changesConventionUsed = files.length > 0;
        if (changesConventionUsed) {
            deployMetricsCollector.addMetric("changesConventionUsed", true);
        }

        return changesConventionUsed;
    }
}
