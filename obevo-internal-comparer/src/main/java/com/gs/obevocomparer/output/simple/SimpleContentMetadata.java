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
package com.gs.obevocomparer.output.simple;

import com.gs.obevocomparer.output.CatoContentMetadata;

public class SimpleContentMetadata implements CatoContentMetadata {

    private final String contentName;
    private final int headerRowCount;
    private final int headerColCount;

    public SimpleContentMetadata(String contentName, int headerRowCount, int headerColCount) {
        this.contentName = contentName;
        this.headerRowCount = headerRowCount;
        this.headerColCount = headerColCount;
    }

    public String getContentName() {
        return this.contentName;
    }

    public int getHeaderRowCount() {
        return this.headerRowCount;
    }

    public int getHeaderColCount() {
        return this.headerColCount;
    }
}
