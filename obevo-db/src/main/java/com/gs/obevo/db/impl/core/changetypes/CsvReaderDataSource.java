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
import org.apache.commons.io.IOUtils;
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
    private final int csvVersion;
    private CSVParser csvreaderV2;
    private Iterator<CSVRecord> iteratorV2;
    private au.com.bytecode.opencsv.CSVReader csvreaderV1;
    private MutableList<String> fields;
    private boolean initialized = false;

    public CsvReaderDataSource(int csvVersion, String name, Reader reader, char delim, Function<String, String> convertDbObjectName, String nullToken) {
        super(name, new NoOpTypeConverter());
        this.csvVersion = csvVersion;
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
                MutableList<String> fields;
                if (csvVersion == CsvStaticDataReader.CSV_V2) {
                    CSVFormat csvFormat = getCsvFormat(delim, nullToken);
                    this.csvreaderV2 = new CSVParser(reader, csvFormat);
                    this.iteratorV2 = csvreaderV2.iterator();
                    fields = ListAdapter.adapt(IteratorUtils.toList(iteratorV2.next().iterator()));
                } else {
                    this.csvreaderV1 = new au.com.bytecode.opencsv.CSVReader(this.reader, this.delim);
                    fields = ArrayAdapter.adapt(this.csvreaderV1.readNext());
                }

                this.fields = fields.collect(this.convertDbObjectName);
            } catch (Exception e) {
                throw new DeployerRuntimeException(e);
            }
            this.initialized = true;
        }
    }

    /**
     * Returns the standard CSV format used by Obevo by both readers (for deploy) and writers (for reverse-engineering) of CSV.
     */
    public static CSVFormat getCsvFormat(char delim, String nullToken) {
        return CSVFormat.newFormat(delim).withRecordSeparator("\r\n").withIgnoreSurroundingSpaces(true).withQuote('"').withEscape('\\').withNullString(nullToken);
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
        IOUtils.closeQuietly(csvreaderV1);
        IOUtils.closeQuietly(csvreaderV2);
    }

    @Override
    protected CatoDataObject nextDataObject() throws Exception {
        if (csvVersion == CsvStaticDataReader.CSV_V2) {
            if (!this.iteratorV2.hasNext()) {
                return null;
            }
            List<String> data = IteratorUtils.toList(iteratorV2.next().iterator());

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

            return dataObject;
        } else {

            String[] data = this.csvreaderV1.readNext();

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

            return dataObject;
        }
    }

    private static class NoOpTypeConverter implements CatoTypeConverter {
        @Override
        public Object convert(Object data) {
            return data;
        }
    }
}
