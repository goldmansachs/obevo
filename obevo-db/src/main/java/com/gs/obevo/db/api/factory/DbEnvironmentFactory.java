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
package com.gs.obevo.db.api.factory;

import com.gs.obevo.api.factory.Obevo;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import org.eclipse.collections.api.collection.MutableCollection;

/**
 * Factory class that facilitates reading environments from a given input file path.
 *
 * @deprecated use the {@link Obevo} API, deprecated since 6.5.0
 * @since 6.0.0
 */
@Deprecated
public class DbEnvironmentFactory {
    private static final DbEnvironmentFactory INSTANCE = new DbEnvironmentFactory();

    public static DbEnvironmentFactory getInstance() {
        return INSTANCE;
    }

    protected DbEnvironmentFactory() {}

    public MutableCollection<DbEnvironment> readFromSourcePath(String sourcePath, String... envNames) {
        return Obevo.<DbEnvironment>readEnvironments(sourcePath, envNames).toList();
    }

    public DbEnvironment readOneFromSourcePath(String sourcePath, String... envNames) {
        return Obevo.readEnvironment(sourcePath, envNames);
    }
}
