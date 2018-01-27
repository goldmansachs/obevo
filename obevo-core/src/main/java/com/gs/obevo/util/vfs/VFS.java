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

import java.io.File;

import org.apache.commons.vfs2.FileSystemException;

public class VFS {
    public static VFS getManager() {
        return new VFS();
    }

    public FileObject resolveFile(String path) {
        try {
            return FileObject.TO_DA_FILE_OBJECT.valueOf(org.apache.commons.vfs2.VFS.getManager().resolveFile(path));
        } catch (FileSystemException e) {
            throw new VFSFileSystemException("Cannot find file " + path + ", or tried to resolve a classpath folder that had no files in it", e);
        }
    }

    public FileObject resolveFile(File baseFile, String name) {
        try {
            return FileObject.TO_DA_FILE_OBJECT.valueOf(org.apache.commons.vfs2.VFS.getManager().resolveFile(baseFile,
                    name));
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    public FileObject resolveFile(FileObject baseFile, String name) {
        try {
            return FileObject.TO_DA_FILE_OBJECT.valueOf(org.apache.commons.vfs2.VFS.getManager().resolveFile(baseFile,
                    name));
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }
}
