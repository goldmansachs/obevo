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
package com.gs.obevo.db.impl.platforms.sybaseiq.iqload;

public enum IqLoadMode {
    IQ(true, false, false, false),
    IQ_CLIENT(false, true, false, false),
    IQ_CLIENT_WINDOWS(false, true, true, false),
    IQ_FTP(true, false, false, true),
    IQ_FTP_WINDOWS(true, false, true, true),;

    private final boolean requiresCoreLoadOptions;
    private final boolean clientLoadEnabled;
    private final boolean convertToWindowsFileSyntax;
    private final boolean useFtpLoad;

    IqLoadMode(boolean requiresCoreLoadOptions, boolean clientLoadEnabled,
            boolean convertToWindowsFileSyntax, boolean useFtpLoad) {
        this.requiresCoreLoadOptions = requiresCoreLoadOptions;
        this.clientLoadEnabled = clientLoadEnabled;
        this.convertToWindowsFileSyntax = convertToWindowsFileSyntax;
        this.useFtpLoad = useFtpLoad;
    }

    public boolean isRequiresCoreLoadOptions() {
        return this.requiresCoreLoadOptions;
    }

    public boolean isClientLoadEnabled() {
        return this.clientLoadEnabled;
    }

    public boolean isConvertToWindowsFileSyntax() {
        return this.convertToWindowsFileSyntax;
    }

    public boolean isUseFtpLoad() {
        return this.useFtpLoad;
    }
}