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
package com.gs.obevocomparer.output.html;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.gs.obevocomparer.output.CatoContentWriter;
import com.gs.obevocomparer.output.simple.AbstractMultiComparisonWriter;
import com.gs.obevocomparer.util.IoUtil;

public class HTMLComparisonWriter extends AbstractMultiComparisonWriter {
    boolean writeToFile;
    private String contentFileName;
    private String summaryContentFileName;
    private final HTMLContentWriter contentWriter;
    private final HTMLContentWriter summaryContentWriter;

    public HTMLComparisonWriter(String contentfile, String summaryContentFile) {
        this(contentfile, summaryContentFile, true, true, true, true);
        this.writeToFile = true;
        this.contentFileName = contentfile;
        this.summaryContentFileName = summaryContentFile;
    }

    public HTMLComparisonWriter(String contentfile, String summaryContentFile,
            boolean writeSummary, boolean writeLegend, boolean writeBreaks,
            boolean writeDataSets) {
        this(writeSummary, writeLegend, writeBreaks, writeDataSets);
        this.writeToFile = true;
        this.contentFileName = contentfile;
        this.summaryContentFileName = summaryContentFile;
    }

    public HTMLComparisonWriter() {
        this(true, true, true, true);
    }

    public HTMLComparisonWriter(boolean writeSummary, boolean writeLegend,
            boolean writeBreaks, boolean writeDataSets) {
        super(writeSummary, writeLegend, writeBreaks, writeDataSets);
        this.writeToFile = false;
        this.contentWriter = new HTMLContentWriter();
        this.summaryContentWriter = new HTMLContentWriter();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.gs.obevocomparer.output.CatoComparisonWriter#close()
     */
    public void close() throws IOException {
        if (this.contentFileName != null) {
            Writer cwriter = new OutputStreamWriter(
                    IoUtil.getOutputStream(new File(this.contentFileName)));
            cwriter.write(this.contentWriter.getHtmlString().toString());
            cwriter.close();
        }
        if (this.summaryContentFileName != null) {
            Writer swriter = new OutputStreamWriter(
                    IoUtil.getOutputStream(this.summaryContentFileName));
            swriter.write(this.summaryContentWriter.getHtmlString().toString());
            swriter.close();
        }
    }

    @Override
    protected CatoContentWriter getSummaryContentWriter() {
        return this.summaryContentWriter;
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
        return this.contentWriter;
    }

    @Override
    protected CatoContentWriter getRightDataSetContentWriter() {
        return this.contentWriter;
    }

    public String getContent() {
        return this.contentWriter.getHtmlString().toString();
    }

    public String getSummaryContent() {
        return this.summaryContentWriter.getHtmlString().toString();
    }
}
