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
package com.gs.obevo.apps.reveng;

import com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder;
import com.gs.obevo.api.factory.EnvironmentEnricher;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeImpl;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.api.platform.Platform;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Lists;

/**
 * Mock DB platform solely for use in the reverse-engineering, which mainly needs access to the change types.
 */
public class TestRevengDbPlatform implements Platform {
    private final ImmutableList<ChangeType> changeTypes = Lists.immutable.<ChangeType>of(
            ChangeTypeImpl.newChangeType("TABLE", false, 1).build(),
            ChangeTypeImpl.newChangeType("SP", true, 2).build()
    );

    @Override
    public String getName() {
        return "TEST_REVENG";
    }

    @Override
    public Function<String, String> convertDbObjectName() {
        return StringFunctions.toUpperCase();
    }

    @Override
    public Class<? extends DeployerAppContext> getAppContextBuilderClass() {
        return null;
    }

    @Override
    public ImmutableList<ChangeType> getChangeTypes() {
        return changeTypes;
    }

    @Override
    public ChangeType getChangeType(final String name) {
        return getChangeTypes().detect(new Predicate<ChangeType>() {
            @Override
            public boolean accept(ChangeType each) {
                return name.equalsIgnoreCase(each.getName());
            }
        });
    }

    @Override
    public boolean hasChangeType(String name) {
        return false;
    }

    @Override
    public boolean isDropOrderRequired() {
        return false;
    }

    @Override
    public ObjectTypeAndNamePredicateBuilder getObjectExclusionPredicateBuilder() {
        return null;
    }

    @Override
    public ImmutableSet<String> getAcceptedExtensions() {
        return null;
    }

    @Override
    public EnvironmentEnricher getEnvironmentEnricher() {
        return null;
    }

    @Override
    public AbstractReveng getDdlReveng() {
        return null;
    }
}
