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
package com.gs.obevo.mongodb.impl;

import com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder;
import com.gs.obevo.api.factory.EnvironmentEnricher;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeImpl;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.api.platform.Platform;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;

public class MongoDbPlatform implements Platform {
    private final ImmutableList<ChangeType> changeTypes;

    public MongoDbPlatform() {
        this.changeTypes = Lists.immutable.<ChangeType>of(
                ChangeTypeImpl.newChangeType("COLLECTION", true, 0).build(),
                ChangeTypeImpl.newChangeType(ChangeType.MIGRATION_STR, false, 100).setEnrichableForDependenciesInText(false).build()
        );
    }

    @Override
    public Class<? extends DeployerAppContext> getAppContextBuilderClass() {
        return MongoDbDeployerAppContext.class;
    }

    @Override
    public String getName() {
        return "MONGODB";
    }

    @Override
    public ImmutableList<ChangeType> getChangeTypes() {
        return changeTypes;
    }

    @Override
    public ChangeType getChangeType(String name) {
        return getChangeTypes().detect(Predicates.attributeEqual(ChangeType.TO_NAME, name));
    }

    @Override
    public boolean hasChangeType(String name) {
        return getChangeTypes().anySatisfy(Predicates.attributeEqual(ChangeType.TO_NAME, name));
    }

    @Override
    public boolean isDropOrderRequired() {
        return false;
    }

    @Override
    public Function<String, String> convertDbObjectName() {
        return Functions.getStringPassThru();
    }

    @Override
    public ObjectTypeAndNamePredicateBuilder getObjectExclusionPredicateBuilder() {
        return new ObjectTypeAndNamePredicateBuilder(ObjectTypeAndNamePredicateBuilder.FilterType.EXCLUDE);
    }

    @Override
    public ImmutableSet<String> getAcceptedExtensions() {
        return Sets.immutable.of("js");
    }

    @Override
    public EnvironmentEnricher getEnvironmentEnricher() {
        return new MongoDbEnvironmentEnricher();
    }
}
