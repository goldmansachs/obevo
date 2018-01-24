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
package com.gs.obevo.api.platform;

import com.gs.obevo.util.vfs.FileObject;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;

public class FileSourceParams {
    private final RichIterable<FileObject> files;
    private final ImmutableSet<String> schemaNames;
    private final boolean baseline;
    private final ImmutableList<ChangeType> changeTypes;
    private final ImmutableSet<String> acceptedExtensions;
    private final String defaultSourceEncoding;
    private final boolean legacyDirectoryStructureEnabled;

    public FileSourceParams(RichIterable<FileObject> files, ImmutableSet<String> schemaNames, boolean baseline, ImmutableList<ChangeType> changeTypes, ImmutableSet<String> acceptedExtensions, String defaultSourceEncoding, boolean legacyDirectoryStructureEnabled) {
        this.files = files;
        this.schemaNames = schemaNames;
        this.baseline = baseline;
        this.changeTypes = changeTypes;
        this.acceptedExtensions = acceptedExtensions;
        this.defaultSourceEncoding = defaultSourceEncoding;
        this.legacyDirectoryStructureEnabled = legacyDirectoryStructureEnabled;
    }

    public RichIterable<FileObject> getFiles() {
        return files;
    }

    public ImmutableSet<String> getSchemaNames() {
        return schemaNames;
    }

    public boolean isBaseline() {
        return baseline;
    }

    public ImmutableList<ChangeType> getChangeTypes() {
        return changeTypes;
    }

    public ImmutableSet<String> getAcceptedExtensions() {
        return acceptedExtensions;
    }

    public String getDefaultSourceEncoding() {
        return defaultSourceEncoding;
    }

    public boolean isLegacyDirectoryStructureEnabled() {
        return legacyDirectoryStructureEnabled;
    }


    public static FileSourceParamsBuilder newBuilder() {
        return new FileSourceParamsBuilder();
    }

    public static class FileSourceParamsBuilder {
        private RichIterable<FileObject> files;
        private ImmutableSet<String> schemaNames;
        private boolean baseline;
        private ImmutableList<ChangeType> changeTypes;
        private ImmutableSet<String> acceptedExtensions;
        private String defaultSourceEncoding;
        private boolean legacyDirectoryStructureEnabled;

        private FileSourceParamsBuilder() {
        }

        public FileSourceParamsBuilder setFiles(RichIterable<FileObject> files) {
            this.files = files;
            return this;
        }

        public FileSourceParamsBuilder setSchemaNames(ImmutableSet<String> schemaNames) {
            this.schemaNames = schemaNames;
            return this;
        }

        public FileSourceParamsBuilder setBaseline(boolean baseline) {
            this.baseline = baseline;
            return this;
        }

        public FileSourceParamsBuilder setChangeTypes(ImmutableList<ChangeType> changeTypes) {
            this.changeTypes = changeTypes;
            return this;
        }

        public FileSourceParamsBuilder setAcceptedExtensions(ImmutableSet<String> acceptedExtensions) {
            this.acceptedExtensions = acceptedExtensions;
            return this;
        }

        public FileSourceParamsBuilder setDefaultSourceEncoding(String defaultSourceEncoding) {
            this.defaultSourceEncoding = defaultSourceEncoding;
            return this;
        }

        public FileSourceParamsBuilder setLegacyDirectoryStructureEnabled(boolean legacyDirectoryStructureEnabled) {
            this.legacyDirectoryStructureEnabled = legacyDirectoryStructureEnabled;
            return this;
        }

        public FileSourceParams build() {
            return new FileSourceParams(files, schemaNames, baseline, changeTypes, acceptedExtensions, defaultSourceEncoding, legacyDirectoryStructureEnabled);
        }
    }}
