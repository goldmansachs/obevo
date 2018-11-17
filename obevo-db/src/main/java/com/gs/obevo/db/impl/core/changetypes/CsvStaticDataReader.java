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
package com.gs.obevo.db.impl.core.changetypes;

import java.io.StringReader;
import java.util.UUID;

import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.dbmetadata.api.DaColumn;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.input.CatoDerivedField;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.csv.CSVFormat;
import org.eclipse.collections.api.block.function.Function;

public class CsvStaticDataReader {
    static final int CSV_V1 = 1;  // 1 is the original
    public static final int CSV_V2 = 2;  // 2 is latest that fixes a number of CSV issues

    /**
     * Returns the standard CSV format used by Obevo by both readers (for deploy) and writers (for reverse-engineering) of CSV.
     */
    public static CSVFormat getCsvFormat(char delim, String nullToken) {
        return CSVFormat.newFormat(delim).withRecordSeparator("\r\n").withIgnoreSurroundingSpaces(true).withQuote('"').withEscape('\\').withNullString(nullToken);
    }

    CsvReaderDataSource getFileDataSource(int csvVersion, DaTable table, String content, char dataDelimiter, String nullToken, Function<String, String> convertDbObjectName) {
        CsvReaderDataSource fileSource = new CsvReaderDataSource(csvVersion, "fileSource", new StringReader(content),
                dataDelimiter, convertDbObjectName, nullToken);
        ConvertUtilsBean cub = new ConvertUtilsBean();
        for (DaColumn col : table.getColumns()) {
            Class targetClassName;

            // This is to handle cases in Sybase ASE where a column comes back in quotes, e.g. "Date"
            // This happens if the column name happens to be a keyword, e.g. for Date
            String columnName = col.getName();
            if (columnName.startsWith("\"") && columnName.endsWith("\"")) {
                columnName = columnName.substring(1, columnName.length() - 1);
            }
            try {
                if (col.getColumnDataType().getTypeClassName().equalsIgnoreCase("byte")) {
                    // this is to handle "tinyint"
                    targetClassName = Integer.class;
                } else if (col.getColumnDataType().getName().equalsIgnoreCase("uuid")) {
                    // handling UUID (first seen in Postgres)
                    targetClassName = UUID.class;
                } else {
                    targetClassName = Class.forName(col.getColumnDataType().getTypeClassName());
                }
            } catch (ClassNotFoundException e) {
                throw new DeployerRuntimeException(e);
            }
            fileSource.addDerivedField(new MyDerivedField(csvVersion, convertDbObjectName.valueOf(columnName),
                    targetClassName, cub, nullToken));
        }

        fileSource.init();  // initialize so that we can discover the fields in the file
        return fileSource;
    }

    private static class MyDerivedField implements CatoDerivedField {
        private final int csvVersion;
        private final String field;
        private final ConvertUtilsBean cub;
        private final Class targetClass;
        private final String nullToken;

        MyDerivedField(int csvVersion, String field, Class targetClass, ConvertUtilsBean cub, String nullToken) {
            this.csvVersion = csvVersion;
            this.field = field;
            this.cub = cub;
            this.targetClass = targetClass;
            this.nullToken = nullToken;
        }

        @Override
        public String getName() {
            return this.field;
        }

        @Override
        public Object getValue(CatoDataObject obj) {
            Object value = obj.getValue(this.field);

            // if we have a null token and the target is of type string, we need to explicitly treat the blank input
            // (which comes back as
            // null in opencsv and cato) as a "", and not a null
            if (csvVersion == CSV_V1) {
                if (this.nullToken != null && this.targetClass.equals(String.class)) {
                    if (value == null) {
                        value = "";
                    }
                }
            }

            if (value == null) {
                return null;
            } else if (!this.targetClass.equals(String.class) && value.equals("")) {
                return null;
            } else if (this.targetClass.equals(UUID.class) && value instanceof String) {
                return UUID.fromString((String) value);
            } else if (csvVersion == CSV_V1 && this.nullToken != null && value.equals(this.nullToken)) {
                // regardless of the output type, if the input was the null token string, we return null here
                return null;
            } else {
                return this.cub.convert(value.toString(), this.targetClass);
            }
        }
    }
}
