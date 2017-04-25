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
package com.gs.obevocomparer.input;

import java.util.ArrayList;
import java.util.Collection;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.data.CatoDataSchema;
import com.gs.obevocomparer.input.converter.StringTypeConverter;
import com.gs.obevocomparer.util.CatoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCatoDataSource implements CatoDataSource {

    private String name;
    private final String shortName;
    private boolean sorted = false;

    private CatoDataSchema dataSchema;
    private CatoTypeConverter typeConverter;

    private final Collection<CatoDerivedField> derivedFields = new ArrayList<CatoDerivedField>();

    private boolean open = false;
    private boolean closed = false;
    private CatoDataObject nextObj = null;
    private int count = 0;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractCatoDataSource.class);

    protected AbstractCatoDataSource(String name, CatoTypeConverter typeConverter) {
        this(name, calculateShortName(name), typeConverter);
    }

    protected AbstractCatoDataSource(String name, String shortName, CatoTypeConverter typeConverter) {
        this.name = name.trim();
        this.shortName = shortName.trim();
        this.typeConverter = typeConverter;
    }

    /**
     * @deprecated replaced by {@link #AbstractCatoDataSource(String, CatoTypeConverter)}
     */
    @Deprecated
    protected AbstractCatoDataSource(String name) {
        this(name, new StringTypeConverter());
    }

    protected abstract void openSource() throws Exception;

    protected abstract void closeSource() throws Exception;

    protected abstract CatoDataObject nextDataObject() throws Exception;

    public final CatoDataObject next() {
        if (!this.open || this.closed) {
            throw new UnsupportedOperationException(
                    "Cannot iterate over a DataSource before calling open() or after calling close()");
        }

        try {
            CatoDataObject current = this.nextObj;

            if (this.typeConverter != null) {
                for (String field : current.getFields()) {
                    current.setValue(field, this.typeConverter.convert(current.getValue(field)));
                }
            }

            for (CatoDerivedField field : this.derivedFields) {
                current.setValue(field.getName(), field.getValue(current));
            }

            this.nextObj = this.nextDataObject();
            this.count++;
            return current;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get next DataObject at row # " + this.count + " in the source", ex);
        }
    }

    public final boolean hasNext() {
        return this.nextObj != null;
    }

    public void remove() {
        throw new UnsupportedOperationException("Cannot remove data from a DataSource");
    }

    @Override
    public final void open() {
        try {
            if (this.open) {
                throw new UnsupportedOperationException("DataSource is already open");
            }
            if (this.closed) {
                throw new UnsupportedOperationException("DataSource is already closed");
            }

            LOG.info("Opening DataSource {}", this.name);
            this.openSource();
            this.open = true;

            this.nextObj = this.nextDataObject();
        } catch (Exception ex) {
            throw new RuntimeException("Could not open DataSource '" + this.name + "'", ex);
        }
    }

    public final void close() {
        try {
            if (!this.open) {
                throw new UnsupportedOperationException("DataSource is not open");
            }
            if (this.closed) {
                throw new UnsupportedOperationException("DataSource is already closed");
            }

            LOG.info("Closing DataSource {}", this.name);
            this.closeSource();
            this.closed = true;
            this.open = false;
        } catch (Exception ex) {
            throw new RuntimeException("Could not close DataSource '" + this.name + "'", ex);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getShortName() {
        return this.shortName;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void addDerivedField(CatoDerivedField derivedField) {
        this.derivedFields.add(derivedField);
    }

    @Override
    public boolean isSorted() {
        return this.sorted;
    }

    @Override
    public void setSorted(boolean sorted) {
        this.sorted = sorted;
    }

    @Override
    public void setTypeConverter(CatoTypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    public void setDataSchema(CatoDataSchema dataSchema) {
        this.dataSchema = dataSchema;
    }

    @Override
    public void setCatoConfiguration(CatoConfiguration configuration) {
        this.dataSchema = configuration.dataSchema();
    }

    protected CatoDataObject createDataObject() {
        return this.dataSchema.createDataObject();
    }

    private static String calculateShortName(String name) {
        name = name.trim();

        if (!name.matches(".+\\s.+")) {
            return name;
        }

        return name.replaceFirst("\\s.+", "");
    }
}
