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
package com.gs.obevo.util.vfs;

import org.apache.commons.vfs2.FileFilter;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileType;

public class BasicFileSelector implements FileSelector {
    private final FileFilter fileFilter;
    private final FileFilter directoryFilter;
    private final boolean traverseDescendents;

    public BasicFileSelector(FileFilter fileFilter) {
        this(fileFilter, true);
    }

    public BasicFileSelector(FileFilter fileFilter, boolean traverseDescendents) {
        this.fileFilter = fileFilter;
        this.directoryFilter = null;
        this.traverseDescendents = traverseDescendents;
    }

    public BasicFileSelector(FileFilter fileFilter, FileFilter directoryFilter) {
        this.fileFilter = fileFilter;
        this.directoryFilter = directoryFilter;
        this.traverseDescendents = false;
    }

    @Override
    public boolean includeFile(FileSelectInfo fileInfo) throws Exception {
        return fileInfo.getDepth() > 0 && this.fileFilter.accept(fileInfo);
    }

    @Override
    public boolean traverseDescendents(FileSelectInfo fileInfo) throws Exception {
        if (fileInfo.getFile().getType() == FileType.FOLDER && fileInfo.getDepth() == 0) {
            return true;
        } else if (this.directoryFilter != null) {
            return this.directoryFilter.accept(fileInfo);
        } else {
            return this.traverseDescendents;
        }
    }
}
