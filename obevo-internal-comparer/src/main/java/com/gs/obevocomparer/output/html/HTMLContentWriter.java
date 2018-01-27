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
package com.gs.obevocomparer.output.html;

import java.io.IOException;

import com.gs.obevocomparer.output.CatoContentMetadata;
import com.gs.obevocomparer.output.CatoContentRow;
import com.gs.obevocomparer.output.CatoContentWriter;

/**
 * <p>
 * Creates an html formatted output for the content. The entire output is a
 * table and each content row is a row in the table.
 * </p>
 */
public class HTMLContentWriter implements CatoContentWriter {
    private final StringBuffer htmlString;
    private CatoContentMetadata contentMetadata;

    private long rowCount;

    /**
     * Initializes the html string
     */
    public HTMLContentWriter() {
        this.rowCount = 0;
        this.htmlString = new StringBuffer();
        this.htmlString.append("<html><head><link rel=\"stylesheet\" href=\"css/cato-output.css\" type=\"text/css\"></head><body>");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.gs.obevocomparer.output.CatoContentWriter#writeRow(com.gs.obevocomparer.output.
     * CatoContentRow)
     */
    public void writeRow(CatoContentRow row) throws IOException {
        this.htmlString.append("<tr class=\"cato-content-row\" id=\"cato-content-row-").append(this.rowCount).append("\">");
        for (int i = 0; i < row.getSize(); i++) {
            this.htmlString.append("<td id=\"cato-content-row-").append(this.rowCount).append("-").append(i).append("\">");
            String cssClass = "cato-unknown";
            if (row.getValueType(i) != null) {
                switch (row.getValueType(i)) {
                case KEY:
                    cssClass = "cato-key";
                    break;
                case FIELD_BREAK:
                    cssClass = "cato-field-break";
                    break;
                case RIGHT_VALUE:
                    cssClass = "cato-right-value";
                    break;
                case RIGHT_ONLY:
                    cssClass = "cato-right-only";
                    break;
                case LEFT_ONLY:
                    cssClass = "cato-left-only";
                    break;
                case EXCLUDE:
                    cssClass = "cato-exclude";
                    break;
                case TITLE:
                    cssClass = "cato-title";
                    break;
                }
            }
            if (row.getValue(i) == null) {
                this.htmlString.append("<div class=\"").append(cssClass).append("\">").append("-").append("</div>");
            } else {
                this.htmlString.append("<div class=\"").append(cssClass).append("\">").append(row.getValue(i)).append("</div>");
            }
            this.htmlString.append("</td>");
        }
        this.htmlString.append("</tr>");
        this.rowCount++;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.gs.obevocomparer.output.CatoContentWriter#openContent(com.gs.obevocomparer.output.
     * CatoContentMetadata)
     */
    public void openContent(CatoContentMetadata contentMetadata) {
        this.contentMetadata = contentMetadata;
        this.htmlString.append("<table class=\"cato-output-table\">");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.gs.obevocomparer.output.CatoContentWriter#closeContent()
     */
    public void closeContent() throws IOException {
        this.htmlString.append("</table></body></html>");
        this.rowCount = 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.gs.obevocomparer.output.CatoContentWriter#closeWriter()
     */
    public void closeWriter() throws IOException {
    }

    public StringBuffer getHtmlString() {
        return this.htmlString;
    }
}
