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
package com.gs.obevo.dbmetadata.impl;

import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DaPackage;
import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaRule;
import com.gs.obevo.dbmetadata.api.DaSequence;
import com.gs.obevo.dbmetadata.api.DaSynonym;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DaUserType;
import com.gs.obevo.dbmetadata.api.RuleBinding;
import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Routine;
import schemacrawler.schema.Sequence;
import schemacrawler.schema.Synonym;
import schemacrawler.schema.Table;
import schemacrawler.schema.View;

public class DaCatalogImpl implements DaCatalog {
    private final Catalog delegate;
    private final ImmutableCollection<DaRoutine> extraRoutines;
    private final Multimap<String, ExtraIndexInfo> extraIndexes;
    private final MapIterable<String, ExtraRerunnableInfo> extraViewInfoMap;
    private final DaRoutineType routineOverrideValue;
    private final SchemaStrategy schemaStrategy;
    private final ImmutableCollection<DaUserType> userTypes;
    private final ImmutableCollection<DaRule> rules;
    private final ImmutableCollection<RuleBinding> ruleBindings;
    private final ImmutableCollection<DaPackage> packages;

    public DaCatalogImpl(Catalog delegate, SchemaStrategy schemaStrategy, ImmutableCollection<DaUserType> userTypes, ImmutableCollection<DaRule> rules, ImmutableCollection<RuleBinding> ruleBindings, ImmutableCollection<DaRoutine> extraRoutines, Multimap<String, ExtraIndexInfo> extraIndexes, ImmutableCollection<ExtraRerunnableInfo> extraViewInfo, DaRoutineType routineOverrideValue, ImmutableCollection<DaPackage> packages) {
        this.delegate = Validate.notNull(delegate);
        this.userTypes = userTypes;
        this.rules = rules;
        this.ruleBindings = ruleBindings;
        this.extraRoutines = extraRoutines;
        this.extraIndexes = extraIndexes;
        this.extraViewInfoMap = extraViewInfo.toMap(ExtraRerunnableInfo.TO_NAME, Functions.<ExtraRerunnableInfo>getPassThru());
        this.routineOverrideValue = routineOverrideValue;
        this.schemaStrategy = schemaStrategy;
        this.packages = packages;
    }

    @Override
    public ImmutableCollection<DaTable> getTables() {
        return CollectionAdapter.adapt(delegate.getTables())
                .collect(new Function<Table, DaTable>() {
                    @Override
                    public DaTable valueOf(Table object) {
                        if (object instanceof View) {
                            return new DaViewImpl((View) object, schemaStrategy, extraViewInfoMap.get(object.getName()));
                        } else {
                            return new DaTableImpl(object, schemaStrategy, extraIndexes);
                        }
                    }
                })
                .toImmutable();
    }

    @Override
    public ImmutableCollection<DaRoutine> getRoutines() {
        return extraRoutines
                .newWithAll(CollectionAdapter.adapt(delegate.getRoutines()).collect(new Function<Routine, DaRoutine>() {
                    @Override
                    public DaRoutine valueOf(Routine object) {
                        return new DaRoutine6Impl(object, schemaStrategy, routineOverrideValue);
                    }
                }));
    }

    @Override
    public ImmutableCollection<DaSequence> getSequences() {
        return CollectionAdapter.adapt(delegate.getSequences()).collect(new Function<Sequence, DaSequence>() {
            @Override
            public DaSequence valueOf(Sequence object) {
                return new DaSequence6Impl(object, schemaStrategy);
            }
        }).toImmutable();
    }

    @Override
    public ImmutableCollection<DaUserType> getUserTypes() {
        return userTypes;
    }

    public ImmutableCollection<DaRule> getRules() {
        return rules;
    }

    @Override
    public ImmutableCollection<RuleBinding> getRuleBindings() {
        return ruleBindings;
    }

    @Override
    public ImmutableCollection<DaPackage> getPackages() {
        return packages;
    }

    @Override
    public ImmutableCollection<DaSynonym> getSynonyms() {
        return CollectionAdapter.adapt(delegate.getSynonyms()).collect(new Function<Synonym, DaSynonym>() {
            @Override
            public DaSynonym valueOf(Synonym object) {
                return new DaSynonym6Impl(object, schemaStrategy);
            }
        }).toImmutable();
    }
}
