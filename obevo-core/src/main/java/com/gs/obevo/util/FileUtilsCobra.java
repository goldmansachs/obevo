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
package com.gs.obevo.util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.SystemUtils;

/**
 * Wrapper around FileUtils to 1) convert to Caramel lists w/ generic args defined 2) converting Checked exceptions to
 * unchecked
 */
public class FileUtilsCobra {
    public static Function<File, String> toFileName() {
        return new Function<File, String>() {
            @Override
            public String valueOf(File object) {
                return object.getName();
            }
        };
    }

    public static File createTempDir(String tmpDirPrefix) {
        File tmpFile;
        try {
            tmpFile = File.createTempFile(tmpDirPrefix, ".tmp", SystemUtils.getJavaIoTmpDir());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileUtils.deleteQuietly(tmpFile);
        String tmpFilePath = tmpFile.getAbsolutePath();

        return new File(tmpFilePath.substring(0, tmpFilePath.length() - 4)); // trim the extension
    }

    public static MutableList<String> readLines(File file) {
        try {
            return FastList.newList(FileUtils.readLines(file));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void writeLines(File file, Collection<?> lines) {
        try {
            FileUtils.writeLines(file, lines);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static MutableCollection<File> listFiles(File directory, IOFileFilter fileFilter, IOFileFilter dirFilter) {
        return FastList.newList(FileUtils.listFiles(directory, fileFilter, dirFilter));
    }

    public static String readFileToString(File file) {
        try {
            return FileUtils.readFileToString(file);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void writeStringToFile(File file, String data) {
        try {
            FileUtils.writeStringToFile(file, data);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String removeFilePrefix(File targetFile, File prefix) {
        return removeFilePrefix(targetFile.getAbsolutePath(), prefix.getAbsolutePath());
    }

    public static String removeFilePrefix(File targetFile, String prefix) {
        return removeFilePrefix(targetFile.getAbsolutePath(), prefix);
    }

    public static String removeFilePrefix(String targetFile, File prefix) {
        return removeFilePrefix(targetFile, prefix.getAbsolutePath());
    }

    public static String removeFilePrefix(String targetFile, String prefix) {
        if (targetFile.equals(prefix)) {
            return "";
        } else {
            return targetFile.replace('\\', '/').replace(prefix.replace('\\', '/'), "").substring(1);
        }
    }

    public static void copyFile(File srcFile, File destFile) {
        try {
            FileUtils.copyFile(srcFile, destFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyDirectory(File srcDir, File destDir) {
        try {
            FileUtils.copyDirectory(srcDir, destDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void moveFile(File srcFile, File destFile) {
        try {
            FileUtils.moveFile(srcFile, destFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
