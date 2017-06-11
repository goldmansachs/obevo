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
package com.gs.obevo.db.apps.reveng;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployExecutionDao;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.db.impl.core.checksum.DbChecksumDao;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;

public class RevengWriter {
    private final Configuration templateConfig;

    public static Predicate2<File, RevEngDestination> defaultShouldOverwritePredicate() {
        return new Predicate2<File, RevEngDestination>() {
            @Override
            public boolean accept(File mainFile, RevEngDestination dbFileRep) {
                return !mainFile.exists();
            }
        };
    }

    public static Predicate2<File, RevEngDestination> overwriteAllPredicate() {
        return new Predicate2<File, RevEngDestination>() {
            @Override
            public boolean accept(File mainFile, RevEngDestination dbFileRep) {
                return true;
            }
        };
    }

    public static Predicate2<File, RevEngDestination> overwriteForSpecificTablesPredicate(
            final MutableSet<String> tableNames) {
        return new Predicate2<File, RevEngDestination>() {
            @Override
            public boolean accept(File mainFile, RevEngDestination dbFileRep) {
                return !mainFile.exists()
                        || tableNames.collect(StringFunctions.toLowerCase()).contains(
                        dbFileRep.getObjectName().toLowerCase());
            }
        };
    }

    public RevengWriter() {
        this.templateConfig = new Configuration();

        // Where load the templates from:
        templateConfig.setClassForTemplateLoading(RevengWriter.class, "/");

        // Some other recommended settings:
        templateConfig.setDefaultEncoding("UTF-8");
        templateConfig.setLocale(Locale.US);
        templateConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    public void write(Platform platform, MutableList<ChangeEntry> allRevEngDestinations, File outputDir, boolean generateBaseline, Predicate2<File, RevEngDestination> shouldOverwritePredicate, String jdbcUrl, String dbHost, Integer dbPort, String dbServer) {
        outputDir.mkdirs();
        if (shouldOverwritePredicate == null) {
            shouldOverwritePredicate = defaultShouldOverwritePredicate();
        }

        MutableSetMultimap<String, String> coreTablesToExclude = Multimaps.mutable.set.empty();
        coreTablesToExclude.putAll(ChangeType.TABLE_STR, Sets.immutable.with(
                ChangeAuditDao.CHANGE_AUDIT_TABLE_NAME,
                DbChecksumDao.SCHEMA_CHECKSUM_TABLE_NAME,
                DeployExecutionDao.DEPLOY_EXECUTION_TABLE_NAME,
                DeployExecutionDao.DEPLOY_EXECUTION_ATTRIBUTE_TABLE_NAME
        ).collect(platform.convertDbObjectName()));

        Predicates<? super RevEngDestination> objectExclusionPredicate = platform.getObjectExclusionPredicateBuilder()
                .add(coreTablesToExclude.toImmutable())
                .build(
                        Functions.chain(RevEngDestination.TO_DB_OBJECT_TYPE, ChangeType.TO_NAME),
                        RevEngDestination.TO_OBJECT_NAME
                );

        MutableMap<RevEngDestination, MutableList<ChangeEntry>> revEngDestinationMap =
                UnifiedMapWithHashingStrategy.newMap(HashingStrategies.fromFunction(RevEngDestination.TO_IDENTITY));
        for (ChangeEntry allRevEngDestination : allRevEngDestinations.select(Predicates.attributePredicate(ChangeEntry.TO_DESTINATION, objectExclusionPredicate))) {
            MutableList<ChangeEntry> changeEntries = revEngDestinationMap.get(allRevEngDestination.getDestination());
            if (changeEntries == null) {
                changeEntries = Lists.mutable.empty();
                revEngDestinationMap.put(allRevEngDestination.getDestination(), changeEntries);
            }

            changeEntries.add(allRevEngDestination);
        }

        for (Pair<RevEngDestination, MutableList<ChangeEntry>> pair :
                revEngDestinationMap.keyValuesView()) {
            RevEngDestination dest = pair.getOne();

            MutableList<ChangeEntry> changes = pair.getTwo()
                    .toSortedListBy(Functions.firstNotNullValue(ChangeEntry.TO_NAME, Functions.<ChangeEntry, String>getFixedValue("")))
                    .toSortedListBy(ChangeEntry.TO_ORDER);
            MutableList<String> metadataAnnotations = changes.flatCollect(ChangeEntry.TO_METADATA_ANNOTATIONS);
            String metadataString;
            if (metadataAnnotations.isEmpty()) {
                metadataString = "";
            } else {
                metadataString = "//// METADATA " + metadataAnnotations.makeString(" ");
            }
            String mainSql = (metadataString.isEmpty() ? "" : metadataString + "\n")
                    + changes.collect(ChangeEntry.TO_SQL).collect(StringFunctions.trim()).makeString("\n");

            try {
                File mainDestinationFile = dest.getDestinationFile(outputDir, false);
                if (dest.isBaselineEligible()) {
                    if (shouldOverwritePredicate.accept(mainDestinationFile, dest)) {
                        MutableList<String> lines = Lists.mutable.empty();

                        String prevChange = null;

                        if (!metadataString.isEmpty()) {
                            lines.add(metadataString);
                        }
                        for (ChangeEntry changeEntry : changes) {
                            if (prevChange == null || !prevChange.equals(changeEntry.getName())) {
                                lines.add(String.format("//// CHANGE%1$s name=%2$s"
                                        , StringUtils.isNotEmpty(changeEntry.getChangeAnnotation())
                                        ? " " + changeEntry.getChangeAnnotation() : ""
                                        , changeEntry.getName()));
                            }

                            lines.add(changeEntry.getSql().trim());
                            lines.add("");

                            prevChange = changeEntry.getName();
                        }

                        FileUtils.writeStringToFile(mainDestinationFile, lines.makeString("\n"));
                    }
                } else {
                    FileUtils.writeStringToFile(mainDestinationFile, mainSql);
                }

                if (generateBaseline && dest.isBaselineEligible()) {
                    FileUtils.writeStringToFile(dest.getDestinationFile(outputDir, true), mainSql);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        MutableSet<String> schemas = allRevEngDestinations.collect(Functions.chain(ChangeEntry.TO_DESTINATION, RevEngDestination.TO_SCHEMA), Sets.mutable.<String>empty());

        Writer fileWriter = null;
        try {
            Template template = templateConfig.getTemplate("deployer/reveng/system-config-template.xml.ftl");
            fileWriter = new FileWriter(new File(outputDir, "system-config.xml"));

            MutableMap<String, Object> params = Maps.mutable.empty();
            params.put("platform", platform.getName());
            params.put("schemas", schemas);
            params.put("jdbcUrl", jdbcUrl);
            params.put("dbHost", dbHost);
            params.put("dbPort", dbPort != null ? String.valueOf(dbPort) : null);
            params.put("dbServer", dbServer);
            template.process(params, fileWriter);
        } catch (TemplateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(fileWriter);
        }
    }
}
