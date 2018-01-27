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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.gs.obevo.util.VisibleForTesting;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.operations.FileOperations;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.factory.Lists;

/**
 * Wrapper for the FileObject of commons VFS. Can represent either a file-system file or classpath resource file.
 *
 * See FileRetrievalModeTest for the tests on this class.
 */
public class FileObject implements org.apache.commons.vfs2.FileObject {
    public static final Function<org.apache.commons.vfs2.FileObject, FileObject> TO_DA_FILE_OBJECT = new Function<org.apache.commons.vfs2.FileObject, FileObject>() {
        @Override
        public FileObject valueOf(org.apache.commons.vfs2.FileObject fileObject) {
            return fileObject == null ? null : new FileObject(fileObject);
        }
    };

    public static final Function<FileObject, URL> TO_URL_DA = new Function<FileObject, URL>() {
        @Override
        public URL valueOf(FileObject object) {
            return object.getURLDa();
        }
    };

    public static final Function<FileObject, String> TO_EXTENSION = new Function<FileObject, String>() {
        @Override
        public String valueOf(FileObject object) {
            return object.getName().getExtension();
        }
    };

    public static final Function<FileObject, String> TO_BASE_NAME = new Function<FileObject, String>() {
        @Override
        public String valueOf(FileObject object) {
            return object.getName().getBaseName();
        }
    };

    private static final int MAX_BOM_LOOKAHEAD = Math.max(Math.max(ByteOrderMark.UTF_8.length(), ByteOrderMark.UTF_16BE.length()), ByteOrderMark.UTF_16LE.length());

    private final org.apache.commons.vfs2.FileObject fileObject;

    private FileObject(org.apache.commons.vfs2.FileObject fileObject) {
        this.fileObject = fileObject;
    }

    private URI getURI() {
        try {
            // Doing some URL encoding here (e.g. replacing sharp, spaces)
            // TODO want a general way to handle the URL encoding; may need improvements from commons VFS ...
            return new URI(this.getName().getURI()
                    .replace("#", "%23")
                    .replace(" ", "%20")
                    .replace("{", "%7B")
                    .replace("}", "%7D")
            );
        } catch (URISyntaxException e) {
            throw new VFSFileSystemException(new FileSystemException(e));
        }
    }

    public URL getURLDa() {
        try {
            return this.getURI().toURL();
        } catch (MalformedURLException e) {
            throw new VFSFileSystemException(new FileSystemException(e));
        }
    }

    @Override
    public FileName getName() {
        return this.fileObject.getName();
    }

    @Override
    public URL getURL() {
        try {
            return this.fileObject.getURL();
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public boolean exists() {
        try {
            return this.fileObject.exists();
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public boolean isHidden() {
        try {
            return this.fileObject.isHidden();
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public boolean isReadable() {
        try {
            return this.fileObject.isReadable();
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public boolean isWriteable() {
        try {
            return this.fileObject.isWriteable();
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public FileType getType() {
        try {
            return this.fileObject.getType();
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public FileObject getParent() {
        try {
            return FileObject.TO_DA_FILE_OBJECT.valueOf(this.fileObject.getParent());
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public FileSystem getFileSystem() {
        return this.fileObject.getFileSystem();
    }

    @Override
    public FileObject[] getChildren() {
        try {
            return Lists.mutable.with(this.fileObject.getChildren()).collect(TO_DA_FILE_OBJECT).toArray(new FileObject[0]);
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public FileObject getChild(String name) {
        try {
            return FileObject.TO_DA_FILE_OBJECT.valueOf(this.fileObject.getChild(name));
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public FileObject resolveFile(String name, NameScope scope) {
        try {
            return FileObject.TO_DA_FILE_OBJECT.valueOf(this.fileObject.resolveFile(name, scope));
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public FileObject resolveFile(String path) {
        try {
            return FileObject.TO_DA_FILE_OBJECT.valueOf(this.fileObject.resolveFile(path));
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public FileObject[] findFiles(FileSelector selector) {
        try {
            return Lists.mutable.with(this.fileObject.findFiles(selector)).collect(TO_DA_FILE_OBJECT)
                    .toArray(new FileObject[0]);
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public void findFiles(FileSelector selector, boolean depthwise, List<org.apache.commons.vfs2.FileObject> selected) {
        try {
            this.fileObject.findFiles(selector, depthwise, selected);
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public boolean delete() {
        try {
            return this.fileObject.delete();
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public int delete(FileSelector selector) {
        try {
            return this.fileObject.delete(selector);
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public void createFolder() {
        try {
            this.fileObject.createFolder();
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public void createFile() {
        try {
            this.fileObject.createFile();
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public void copyFrom(org.apache.commons.vfs2.FileObject srcFile, FileSelector selector) {
        try {
            this.fileObject.copyFrom(srcFile, selector);
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public void moveTo(org.apache.commons.vfs2.FileObject destFile) {
        try {
            if (destFile instanceof FileObject) {
                destFile = ((FileObject) destFile).fileObject;
            }
            this.fileObject.moveTo(destFile);
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public boolean canRenameTo(org.apache.commons.vfs2.FileObject newfile) {
        return this.fileObject.canRenameTo(newfile);
    }

    @Override
    public FileContent getContent() {
        try {
            return this.fileObject.getContent();
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public void close() {
        try {
            this.fileObject.close();
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public void refresh() {
        try {
            this.fileObject.refresh();
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    @Override
    public boolean isAttached() {
        return this.fileObject.isAttached();
    }

    @Override
    public boolean isContentOpen() {
        return this.fileObject.isContentOpen();
    }

    @Override
    public FileOperations getFileOperations() {
        try {
            return this.fileObject.getFileOperations();
        } catch (FileSystemException e) {
            throw new VFSFileSystemException(e);
        }
    }

    public String getStringContent() {
        return getStringContent(CharsetStrategyFactory.getCharsetStrategy(Charset.defaultCharset()));
    }

    public String getStringContent(CharsetStrategy charsetStrategy) {
        byte[] contentBytes = getContentBytes();
        Charset charset = charsetStrategy.determineCharset(contentBytes);
        if (charset == null) {
            charset = Charset.defaultCharset();
        }
        return new String(filterBomByteIfUtf(charset, contentBytes), charset);
    }

    /**
     * Get the detected charset. Only for use in testing to validate what data we read.
     */
    @VisibleForTesting
    Charset getDetectedCharset() {
        return CharsetStrategyFactory.getDetectCharsetStrategy().determineCharset(getContentBytes());
    }

    private byte[] getContentBytes() {
        try (InputStream inputStream = getURLDa().openStream()) {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Remove BOM character from the bytes if needed for UTF8.
     *
     * We must remove the BOM ourselves per the following JDK bugs.
     * http://bugs.java.com/view_bug.do?bug_id=4508058
     * http://bugs.java.com/view_bug.do?bug_id=6378911
     */
    private byte[] filterBomByteIfUtf(Charset charset, byte[] bytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try {
            if (charset != null) {
                // only try to remove the BOM if the input charset is UTF
                BOMInputStream bomChecker = null;
                if (charset.equals(StandardCharsets.UTF_8)) {
                    bomChecker = new BOMInputStream(inputStream, ByteOrderMark.UTF_8);
                } else if (charset.equals(StandardCharsets.UTF_16LE)) {
                    bomChecker = new BOMInputStream(inputStream, ByteOrderMark.UTF_16LE);
                } else if (charset.equals(StandardCharsets.UTF_16BE)) {
                    bomChecker = new BOMInputStream(inputStream, ByteOrderMark.UTF_16BE);
                }

                inputStream.mark(MAX_BOM_LOOKAHEAD);
                if (bomChecker != null && !bomChecker.hasBOM()) {
                    inputStream.reset();  // hasBOM will advance the input stream. Move it back if the BOM didn't exist
                    // Note - BOMInputStream can't actually be used to read in data; hence, we operate on the original inputStream
                }
            }
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileObject)) {
            return false;
        }

        FileObject that = (FileObject) o;

        if (this.fileObject == null ? that.fileObject != null : !this.fileObject.equals(that.fileObject)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return this.fileObject == null ? 0 : this.fileObject.hashCode();
    }

    @Override
    public String toString() {
        return this.fileObject == null ? "Null file" : this.fileObject.toString();
    }
}
