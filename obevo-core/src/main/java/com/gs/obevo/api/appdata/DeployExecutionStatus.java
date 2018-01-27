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
package com.gs.obevo.api.appdata;

public enum DeployExecutionStatus {
    IN_PROGRESS('I'),
    SUCCEEDED('S'),
    FAILED('F'),;

    private final char statusCode;

    DeployExecutionStatus(char statusCode) {
        this.statusCode = statusCode;
    }

    public char getStatusCode() {
        return statusCode;
    }

    public static DeployExecutionStatus valueOfStatusCode(char statusCode) {
        for (DeployExecutionStatus deployExecutionStatus : values()) {
            if (deployExecutionStatus.getStatusCode() == statusCode) {
                return deployExecutionStatus;
            }
        }

        throw new IllegalArgumentException("No status code [" + statusCode + "] found");
    }
}
