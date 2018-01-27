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
package com.gs.obevocomparer.input.text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.Queue;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.input.AbstractCatoDataSource;
import com.gs.obevocomparer.input.converter.StringTypeConverter;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

public abstract class AbstractStreamDataSource extends AbstractCatoDataSource {

    private final Reader reader;
    MutableList<String> fields = Lists.mutable.empty();

    boolean hasHeader = false;
    private int stripFirst = 0;
    private int stripLast = 0;

    private BufferedReader bufferedReader;
    private Queue<String> lines;

    protected AbstractStreamDataSource(String name, InputStream stream) {
        this(name, new InputStreamReader(stream));
    }

    AbstractStreamDataSource(String name, Reader reader) {
        super(name, new StringTypeConverter());
        this.reader = reader;
    }

    protected CatoDataObject nextDataObject() throws Exception {
        String line = this.bufferedReader.readLine();
        if (line == null) {
            return null;
        }

        this.lines.add(line);
        CatoDataObject dataObject = this.createDataObject();

        String[] data = this.parseData(this.lines.poll());
        for (int i = 0; i < data.length; i++) {
            dataObject.setValue(this.getField(i), data[i]);
        }

        return dataObject;
    }

    protected void openSource() throws Exception {
        if (this.reader instanceof BufferedReader) {
            this.bufferedReader = (BufferedReader) this.reader;
        } else {
            this.bufferedReader = new BufferedReader(this.reader);
        }

        this.lines = new LinkedList<String>();
        String line;

        int stripCount = 0;
        LOG.debug("Stripping {} lines from top of file", this.stripFirst);
        while (stripCount < this.stripFirst && this.bufferedReader.readLine() != null) {
            stripCount++;
        }

        if (this.hasHeader) {
            for (String field : this.parseData(this.bufferedReader.readLine())) {
                this.fields.add(field.trim());
            }
            LOG.debug("Set header fields to {}", this.fields.makeString());
        }

        stripCount = 0;
        LOG.debug("Stripping {} lines from bottom of file", this.stripLast);
        while (stripCount++ < this.stripLast && (line = this.bufferedReader.readLine()) != null) {
            this.lines.add(line);
        }
    }

    protected void closeSource() throws Exception {
        this.bufferedReader.close();
    }

    private String getField(int index) {
        if (this.fields.size() <= index) {
            for (int i = this.fields.size(); i <= index; i++) {
                this.fields.add("Field " + (i + 1));
            }
        }

        return this.fields.get(index);
    }

    protected abstract String[] parseData(String line);

    public boolean hasHeader() {
        return this.hasHeader;
    }

    void setHeader(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }

    public int getStripFirst() {
        return this.stripFirst;
    }

    public void setStripFirst(int stripFirst) {
        this.stripFirst = stripFirst;
    }

    public int getStripLast() {
        return this.stripLast;
    }

    public void setStripLast(int stripLast) {
        this.stripLast = stripLast;
    }
}
