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

import org.apache.commons.vfs2.FileFilter;

public final class FileFilterUtils {
    private FileFilterUtils() {
    }

    public static FileFilter not(FileFilter f) {
        return new NotFileFilter(f);
    }

    public static FileFilter or(FileFilter f1, FileFilter f2) {
        return new OrFileFilter(f1, f2);
    }

    public static FileFilter and(FileFilter f1, FileFilter f2) {
        return new AndFileFilter(f1, f2);
    }

    public static FileFilter and(FileFilter f1, FileFilter f2, FileFilter... rest) {
        return new AndFileFilter(f1, f2, rest);
    }

    public static FileFilter vcsAware() {
        return not(or(
                svnAware(), cvsAware()));
    }

    private static FileFilter svnAware() {
        return and(DirectoryFileFilter.INSTANCE, new NameFileFilter(".svn"));
    }

    private static FileFilter cvsAware() {
        return and(DirectoryFileFilter.INSTANCE, new NameFileFilter("CVS"));
    }

    public static FileFilter directory() {
        return DirectoryFileFilter.INSTANCE;
    }

    public static FileFilter makeVcsAware(FileFilter f) {
        return and(
                f,
                and(DirectoryFileFilter.INSTANCE, new NameFileFilter("CVS")),
                and(DirectoryFileFilter.INSTANCE, new NameFileFilter(".svn")));
    }
}
