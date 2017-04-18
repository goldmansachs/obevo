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
package com.gs.catodeployany.compare.breaks;

import com.gs.catodeployany.compare.CatoDataSide;
import com.gs.catodeployany.data.CatoDataObject;
import com.gs.catodeployany.util.TestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AbstractBreakTest {

    CatoDataObject obj;
    AbstractBreak br;

    @Before
    public void setUp() {
        this.obj = TestUtil.createEmptyDataObject();
    }

    @Test
    public void constructorTest() {
        this.br = new AbstractBreak(AbstractBreakTest.this.obj, CatoDataSide.LEFT) {};
        Assert.assertSame(this.obj, this.br.getDataObject());
        Assert.assertEquals(CatoDataSide.LEFT, this.br.getDataSide());

        this.br = new AbstractBreak(AbstractBreakTest.this.obj, CatoDataSide.RIGHT) {};
        Assert.assertSame(this.obj, this.br.getDataObject());
        Assert.assertEquals(CatoDataSide.RIGHT, this.br.getDataSide());
    }

    @Test
    public void setExcludedTest() {
        this.br = new AbstractBreak(AbstractBreakTest.this.obj, CatoDataSide.LEFT) {};
        Assert.assertFalse(this.br.isExcluded());
        this.br.setExcluded(true);
        Assert.assertTrue(this.br.isExcluded());
    }
}
