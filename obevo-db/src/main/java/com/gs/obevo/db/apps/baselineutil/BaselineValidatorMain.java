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
package com.gs.obevo.db.apps.baselineutil;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DbMetadataComparisonUtil;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.dbmetadata.deepcompare.CompareBreak;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaselineValidatorMain {
    private static final Logger LOG = LoggerFactory.getLogger(BaselineValidatorMain.class);
    private final DbMetadataComparisonUtil dbMetadataComparisonUtil = new DbMetadataComparisonUtil();

    private static final Function<Class, String> CLASS_TO_NAME = new Function<Class, String>() {
        @Override
        public String valueOf(Class object) {
            return object.getName();
        }
    };

    public void validateNoBaselineBreaks(DbDeployerAppContext appContext) {
        validateNoBaselineBreaks(appContext, Predicates.alwaysFalse());
    }

    private void validateNoBaselineBreaks(DbDeployerAppContext appContext, Predicate<? super CompareBreak> breakIgnorePredicate) {
        MutableList<CompareBreak> sortedCompareBreaks = this.calculateBaselineBreaks(appContext).toList().sortThis(
                Comparators.fromFunctions(
                        CompareBreak.TO_COMPARE_SUBJECT,
                        Functions.chain(CompareBreak.TO_CLAZZ, CLASS_TO_NAME),
                        Functions.chain(Functions.getToClass(), CLASS_TO_NAME)
                ));

        MutableList<CompareBreak> relevantBreaks = sortedCompareBreaks.reject(breakIgnorePredicate);

        LOG.info("Found " + relevantBreaks.size() + " breaks");

        if (!relevantBreaks.isEmpty()) {
            throw new IllegalArgumentException(
                    "Found some mismatches between your change alters (LEFT) and your baseline files (RIGHT). Please review:\n"
                            + relevantBreaks.makeString("\n"));
        }
    }

    public ImmutableSet<CompareBreak> calculateBaselineBreaks(DbDeployerAppContext appContext) {
        appContext.setupEnvInfra();

        final ImmutableSet<PhysicalSchema> physicalSchemas = appContext.getEnvironment().getPhysicalSchemas();
        final DbMetadataManager metadataManager = appContext.getDbMetadataManager();

        LOG.info("Starting the first run (normal changes, not the baseline mode)");
        appContext.cleanAndDeploy();

        final MutableMap<PhysicalSchema, ImmutableCollection<DaTable>> regularTableMap = physicalSchemas.toMap(Functions.getPassThru(), object -> {
            DaCatalog database = metadataManager.getDatabase(object,
                    new DaSchemaInfoLevel().setRetrieveTableAndColumnDetails(), true, false);
            return database.getTables().reject(DaTable.IS_VIEW);
        });

        // run 2
        LOG.info("Starting the second run (in the baseline mode)");
        appContext.cleanEnvironment();
        appContext.deploy(new MainDeployerArgs().useBaseline(true));

        LOG.info("Starting the results comparison (LEFT is the regular tables, RIGHT is the baseline)");

        final MutableMap<PhysicalSchema, ImmutableCollection<DaTable>> baselineTableMap = physicalSchemas.toMap(Functions.getPassThru(), object -> {
            DaCatalog database = metadataManager.getDatabase(object,
                    new DaSchemaInfoLevel().setRetrieveTableAndColumnDetails(), true, false);
            return database.getTables().reject(DaTable.IS_VIEW);
        });

        return physicalSchemas.flatCollect(schema -> {
            ImmutableCollection<DaTable> regularTables = regularTableMap.get(schema);
            ImmutableCollection<DaTable> baselineTables = baselineTableMap.get(schema);
            return BaselineValidatorMain.this.dbMetadataComparisonUtil.compareTables(regularTables, baselineTables);
        });
    }
}
