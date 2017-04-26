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
package com.gs.obevocomparer.input.text;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FixedStreamDataSource extends AbstractStreamDataSource {

    final List<FixedField> fixedFields = new ArrayList<FixedField>();

    public FixedStreamDataSource(String name, Reader reader, Object... fieldInput) {
        super(name, reader);
        this.hasHeader = false;
        this.parseFields(fieldInput);
    }

    public void setHeader(boolean hasHeader) {
        throw new UnsupportedOperationException("Cannot set header for fixed width file");
    }

    private void parseFields(Object[] fieldInput) {
        if (fieldInput.length == 0) {
            throw new IllegalArgumentException("Input field list must not be empty");
        }

        if (fieldInput.length % 3 != 0) {
            throw new IllegalArgumentException("Input field list must be of form '[<field>, <start index>, <end index>]*'");
        }

        for (int i = 0; i < fieldInput.length; i += 3) {
            if (!(fieldInput[i] instanceof String) ||
                    !(fieldInput[i + 1] instanceof Integer) ||
                    !(fieldInput[i + 2] instanceof Integer)) {
                throw new IllegalArgumentException("Input field list must be of form '[<field>, <start index>, <end index>]*'");
            }

            this.fixedFields.add(new FixedField((String) fieldInput[i],
                    (Integer) fieldInput[i + 1], (Integer) fieldInput[i + 2]));
        }

        Collections.sort(this.fixedFields);
        this.fields.clear();
        for (FixedField field : this.fixedFields) {
            this.fields.add(field.field);
        }
    }

    protected String[] parseData(String line) {
        String[] data = new String[this.fixedFields.size()];

        for (int i = 0; i < this.fixedFields.size(); i++) {
            if (line.length() <= this.fixedFields.get(i).end) {
                data[i] = line.substring(this.fixedFields.get(i).start);
            } else {
                data[i] = line.substring(this.fixedFields.get(i).start, this.fixedFields.get(i).end);
            }
        }

        return data;
    }

    private static class FixedField implements Comparable<FixedField> {
        public final String field;
        public final int start;
        public final int end;

        public FixedField(String field, int start, int end) {
            this.field = field;
            this.start = start;
            this.end = end;
        }

        public int compareTo(FixedField o) {
            int val = Integer.valueOf(this.start).compareTo(o.start);
            if (val == 0) {
                return Integer.valueOf(this.end).compareTo(o.end);
            } else {
                return val;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FixedField)) {
                return false;
            }

            FixedField that = (FixedField) o;

            if (start != that.start) {
                return false;
            }
            if (end != that.end) {
                return false;
            }
            return !(field != null ? !field.equals(that.field) : that.field != null);
        }

        @Override
        public int hashCode() {
            int result = field != null ? field.hashCode() : 0;
            result = 31 * result + start;
            result = 31 * result + end;
            return result;
        }
    }
}
