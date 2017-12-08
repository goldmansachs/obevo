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

import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.db.impl.platforms.AbstractDbPlatform;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.block.factory.StringFunctions;

/**
 * Mock DB platform solely for use in the reverse-engineering, which mainly needs access to the change types.
 */
public class TestRevengDbPlatform extends AbstractDbPlatform {
    public TestRevengDbPlatform() {
        super("TEST_REVENG");
    }

    @Override
    public Class<? extends DeployerAppContext> initializeAppContextBuilderClass() {
        return null;
    }

    @Override
    protected String initializeDefaultDriverClassName() {
        return null;
    }

    @Override
    public Function<String, String> convertDbObjectName() {
        return StringFunctions.toUpperCase();
    }
}
