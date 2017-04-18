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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum FileRetrievalMode implements FileResolverStrategy {
    FILE_SYSTEM,
    CLASSPATH,;

    private static final Logger LOG = LoggerFactory.getLogger(FileRetrievalMode.class);

    @Override
    public ListIterable<FileObject> resolveFileObjects(String path) {
        switch (this) {
            case FILE_SYSTEM:
                FileObject fileObject = this.getFileObjectFromFileSystem(path);
                return isValidFileObject(fileObject) ? Lists.mutable.of(fileObject)
                                                     : Lists.mutable.<FileObject>empty();
            case CLASSPATH:
                return this.getFileObjectsFromClasspath(path);
            default:
                throw new IllegalArgumentException("Not expecting this enum: " + path);
        }
    }

    public FileObject resolveSingleFileObject(String path) {
        ListIterable<FileObject> fileObjects = resolveFileObjects(path);
        if (fileObjects.size() > 1) {
            LOG.warn(String.format("Path [%s] should resolve to only one source directory but found more " +
                    "than one, please investigate...", path));
        }
        return fileObjects.isEmpty() ? null : fileObjects.getFirst();
    }

    private boolean isValidFileObject(FileObject fileObject) {
        return fileObject != null && fileObject.exists();
    }

    private FileObject getFileObjectFromFileSystem(String path) {
        return VFS.getManager().resolveFile("file:" + new File(path).getAbsolutePath());
    }

    public ListIterable<FileObject> getFileObjectsFromClasspath(String path) {
        MutableList<FileObject> fileObjects = Lists.mutable.empty();

        Collection<URL> resources = getResourcesFromClasspath(path);
        for (URL resource: resources) {
            FileObject fileObject = getFileObjectUsingURL(resource);
            if (isValidFileObject(fileObject)) {
                fileObjects.add(fileObject);
            }
        }

        return fileObjects;
    }

    private FileObject getFileObjectUsingURL(URL resource) {
        String filePath = resource.getPath();
        FileObject fileObject = null;
        if (filePath.contains(".jar!")) {
            filePath = cleanFilePath(filePath);
            fileObject = VFS.getManager().resolveFile("jar:" + new File(filePath).getAbsolutePath());
        } else if (filePath.contains(".zip!")) {
            filePath = cleanFilePath(filePath);
            fileObject = VFS.getManager().resolveFile("zip:" + new File(filePath).getAbsolutePath());
        } else {
            fileObject = VFS.getManager().resolveFile("file:" + new File(filePath).getAbsolutePath());
        }
        return fileObject;
    }

    private String cleanFilePath(String filePath) {
        return filePath.substring(filePath.lastIndexOf("file:") + 5);
    }

    public static List<URL> getResourcesFromClasspath(String path) {
        try {
            return Collections.list(classLoader(path).getResources(path));
        } catch (IOException e) {
            throw new VFSFileSystemException(String.format("Error while resolving file using %s", path), e);
        }
    }

    private static ClassLoader classLoader(String resourcePath) {
        if (FileRetrievalMode.class.getClassLoader().getResource(resourcePath) != null)
            return FileRetrievalMode.class.getClassLoader();

        return Thread.currentThread().getContextClassLoader();
    }

}
