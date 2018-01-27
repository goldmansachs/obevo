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
package com.gs.obevo.impl;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.platform.FileSourceContext;
import com.gs.obevo.api.platform.FileSourceParams;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * Strategy to read the source from the given file parameters.
 */
public class FileSourceReaderStrategy implements SourceReaderStrategy {
    private final FileSourceContext fileSourceContext;
    private final FileSourceParams fileSourceParams;

    public FileSourceReaderStrategy(FileSourceContext fileSourceContext, FileSourceParams fileSourceParams) {
        this.fileSourceContext = fileSourceContext;
        this.fileSourceParams = fileSourceParams;
    }

    @Override
    public ImmutableList<Change> getChanges() {
        return fileSourceContext.readChanges(fileSourceParams);
    }

    @Override
    public ImmutableList<Change> getChanges(boolean useBaseline) {
        return fileSourceContext.readChanges(fileSourceParams);
    }
}
