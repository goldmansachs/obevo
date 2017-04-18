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
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;

public class DirectoryFileFilter implements FileFilter {
    public static final DirectoryFileFilter INSTANCE = new DirectoryFileFilter();

    @Override
    public boolean accept(FileSelectInfo fileInfo) {
        try {
            return fileInfo.getFile().getType() == FileType.FOLDER;
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
    }
}
