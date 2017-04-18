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
package com.gs.catodeployany.sort.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.gs.catodeployany.sort.Sort;

public class MemorySort<T> implements Sort<T> {

    private final Comparator<T> comparator;

    public MemorySort(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    @Override
    public Iterator<T> sort(Iterator<T> data) {
        List<T> dataList = toList(data);
        Collections.sort(dataList, this.comparator);
        return dataList.iterator();
    }

    private List<T> toList(Iterator<T> iterator) {
        List list = new ArrayList();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }
}
