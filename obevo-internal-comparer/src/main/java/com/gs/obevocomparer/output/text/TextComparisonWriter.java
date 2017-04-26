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
package com.gs.obevocomparer.output.text;

import java.io.File;
import java.io.IOException;

import com.gs.obevocomparer.output.CatoContentWriter;
import com.gs.obevocomparer.output.simple.AbstractComparisonWriter;

public class TextComparisonWriter extends AbstractComparisonWriter {

    private final CatoContentWriter contentWriter;

    public TextComparisonWriter(String fileName) throws IOException {
        this(new File(fileName));
    }

    public TextComparisonWriter(File file) throws IOException {
        this(file, true);
    }

    public TextComparisonWriter(File file, boolean writeBreaks) throws IOException {
        super(true, false, writeBreaks, false);
        this.contentWriter = new TextContentWriter(file);
    }

    @Override
    public void close() throws IOException {
        this.contentWriter.closeWriter();
    }

    @Override
    protected CatoContentWriter getSummaryContentWriter() {
        return this.contentWriter;
    }

    @Override
    protected CatoContentWriter getBreakContentWriter() {
        return this.contentWriter;
    }

    @Override
    protected CatoContentWriter getExcludedBreakContentWriter() {
        return this.contentWriter;
    }

    @Override
    protected CatoContentWriter getLeftDataSetContentWriter() {
        return null;
    }

    @Override
    protected CatoContentWriter getRightDataSetContentWriter() {
        return null;
    }
}
 