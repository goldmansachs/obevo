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
package com.gs.obevocomparer.output;

/**
 * This interface represents a single row of formatted output content, ready to be written
 * to Excel/disk/etc.  The row contains a series of values and value types, representing
 * the various types of cells an output may contain.  For writing to Excel, different
 * ValueTypes may correlate to different cell colors.
 */
public interface CatoContentRow {

    enum ValueType {
        KEY, FIELD_BREAK, RIGHT_VALUE, RIGHT_ONLY, LEFT_ONLY, EXCLUDE, TITLE
    }

    Object getValue(int index);

    ValueType getValueType(int index);

    int getSize();
}
