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
package com.gs.obevo.db.impl.core.changetypes;

import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.input.AbstractCatoDataSource;
import com.gs.obevocomparer.input.CatoTypeConverter;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.ListAdapter;

public class CsvReaderDataSource extends AbstractCatoDataSource {
    private final Reader reader;
    private final char delim;
    private final Function<String, String> convertDbObjectName;
    private final String nullToken;
    private CSVParser csvreader;
    private Iterator<CSVRecord> iterator;
/*
    private au.com.bytecode.opencsv.CSVReader csvreader;
*/
    private MutableList<String> fields;
    private boolean initialized = false;

    public CsvReaderDataSource(String name, Reader reader, char delim, Function<String, String> convertDbObjectName, String nullToken) {
        super(name, new NoOpTypeConverter());
        this.reader = reader;
        this.delim = delim;
        this.convertDbObjectName = convertDbObjectName;
        this.nullToken = nullToken;
    }

    /**
     * Putting this init here so that we can discover the file fields before running the actual rec
     */
    public void init() {
        if (!this.initialized) {


            try {
                CSVFormat csvFormat = CSVFormat.newFormat(delim).withIgnoreSurroundingSpaces(true).withQuote('"').withEscape('\\').withNullString(nullToken);
                this.csvreader = new CSVParser(reader, csvFormat);
                this.iterator = csvreader.iterator();
                MutableList<String> fields = ListAdapter.adapt(IteratorUtils.toList(iterator.next().iterator()));
/*
                this.csvreader = new au.com.bytecode.opencsv.CSVReader(this.reader, this.delim);
                MutableList<String> fields = ArrayAdapter.adapt(this.csvreader.readNext());
*/
                this.fields = fields.collect(this.convertDbObjectName);
            } catch (Exception e) {
                throw new DeployerRuntimeException(e);
            }
            this.initialized = true;
        }
    }

    @Override
    protected void openSource() throws Exception {
        this.init();
    }

    public List<String> getFields() {
        return this.fields;
    }

    @Override
    protected void closeSource() throws Exception {
        this.csvreader.close();
    }

    @Override
    protected CatoDataObject nextDataObject() throws Exception {
        if (!this.iterator.hasNext()) {
            return null;
        }
        List<String> data = IteratorUtils.toList(iterator.next().iterator());

        if (data == null || data.size() == 0 || (data.size() == 1 && data.get(0).isEmpty())) {
            return null;
        } else if (data.size() != this.fields.size()) {
            throw new IllegalArgumentException("This row does not have the right # of columns: expecting "
                    + this.fields.size() + " columns, but the row was: " + Lists.mutable.with(data));
        }

        CatoDataObject dataObject = this.createDataObject();
        for (int i = 0; i < data.size(); i++) {
            dataObject.setValue(this.fields.get(i), data.get(i));
        }

/*
        String[] data = this.csvreader.readNext();

        if (data == null || data.length == 0 || (data.length == 1 && data[0].isEmpty())) {
            return null;
        } else if (data.length != this.fields.size()) {
            throw new IllegalArgumentException("This row does not have the right # of columns: expecting "
                    + this.fields.size() + " columns, but the row was: " + Lists.mutable.with(data));
        }

        CatoDataObject dataObject = this.createDataObject();
        for (int i = 0; i < data.length; i++) {
            dataObject.setValue(this.fields.get(i), data[i]);
        }
*/

        return dataObject;
    }

    private static class NoOpTypeConverter implements CatoTypeConverter {
        @Override
        public Object convert(Object data) {
            return data;
        }
    }
}
