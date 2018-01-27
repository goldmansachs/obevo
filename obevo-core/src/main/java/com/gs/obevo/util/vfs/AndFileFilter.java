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
package com.gs.obevo.util.vfs;

import java.util.List;

import org.apache.commons.vfs2.FileFilter;
import org.apache.commons.vfs2.FileSelectInfo;
import org.eclipse.collections.impl.list.mutable.FastList;

class AndFileFilter implements FileFilter {
    private final List<FileFilter> filters;

    AndFileFilter(FileFilter f1, FileFilter f2) {
        this(f1, f2, new FileFilter[0]);
    }

    AndFileFilter(FileFilter f1, FileFilter f2, FileFilter... rest) {
        this.filters = FastList.newListWith(f1, f2).with(rest);
    }

    @Override
    public boolean accept(FileSelectInfo fileInfo) {
        for (FileFilter f : this.filters) {
            if (!f.accept(fileInfo)) {
                return false;
            }
        }
        return true;
    }
}
