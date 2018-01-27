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
package com.gs.obevocomparer.input.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.util.MockDataSource;
import com.gs.obevocomparer.util.TestUtil;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FilterDataSourceTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testEmptyDataSource() {
        Predicate<CatoDataObject> disc = mock(Predicate.class);

        FilterDataSource filterDataSource = new FilterDataSource(new MockDataSource(), disc);

        filterDataSource.open();

        Assert.assertFalse(filterDataSource.hasNext());
        verify(disc, never()).accept(any(CatoDataObject.class));
        try {
            filterDataSource.next();
            Assert.fail();
        } catch (NoSuchElementException ex) {
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFilteringDataSource() {
        MockDataSource dataSource = new MockDataSource();

        List<CatoDataObject> objs = new ArrayList<CatoDataObject>();
        for (int i = 0; i < 6; i++) {
            objs.add(TestUtil.createEmptyDataObject());
            dataSource.addData(objs.get(i));
        }

        Predicate<CatoDataObject> disc = mock(Predicate.class);
        when(disc.accept(any(CatoDataObject.class))).thenReturn(true, false, false, true, true, false);

        FilterDataSource filterDataSource = new FilterDataSource(dataSource, disc);
        filterDataSource.open();

        Assert.assertSame(objs.get(0), filterDataSource.next());
        Assert.assertSame(objs.get(3), filterDataSource.next());
        Assert.assertSame(objs.get(4), filterDataSource.next());

        Assert.assertFalse(filterDataSource.hasNext());

        verify(disc, times(6)).accept(any(CatoDataObject.class));
    }
}
