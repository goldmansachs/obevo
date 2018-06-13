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
package com.gs.obevo.impl.reader

import com.gs.obevo.api.appdata.ChangeInput
import com.gs.obevo.api.platform.FileSourceContext
import com.gs.obevo.api.platform.FileSourceParams
import org.eclipse.collections.api.list.ImmutableList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * DbChangeReader implementation that caches an underlying DbChangeReader based on the useBaseline parameter passed in.
 * The caching is here so that the DbDeployerAppContext can be reused for deployments but only read the file
 * system once to save time. The biggest usecase for this is for unit tests.
 */
class CachedDbChangeReader(
        private val dbChangeReader: FileSourceContext
) : FileSourceContext {

    private val cachedResults: ConcurrentMap<FileSourceParams, ImmutableList<ChangeInput>> = ConcurrentHashMap()

    override fun readChanges(fileSourceParams: FileSourceParams): ImmutableList<ChangeInput> {
        return cachedResults.getOrPut(fileSourceParams, { dbChangeReader.readChanges(fileSourceParams) })
    }
}
