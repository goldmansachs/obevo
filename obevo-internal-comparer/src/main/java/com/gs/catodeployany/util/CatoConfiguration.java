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
package com.gs.catodeployany.util;

import java.util.Comparator;

import com.gs.catodeployany.compare.CatoBreakExcluder;
import com.gs.catodeployany.compare.CatoDataSourceComparator;
import com.gs.catodeployany.compare.CatoProperties;
import com.gs.catodeployany.data.CatoDataObject;
import com.gs.catodeployany.data.CatoDataSchema;
import com.gs.catodeployany.sort.Sort;

public interface CatoConfiguration {
    CatoProperties getProperties();

    CatoDataSourceComparator dataSourceComparator();

    Comparator<CatoDataObject> dataObjectComparator();

    Sort<CatoDataObject> sort();

    CatoBreakExcluder breakExcluder();

    // was declared as prototype
    CatoDataSchema dataSchema();
}
