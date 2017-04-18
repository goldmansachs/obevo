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
import org.junit.BeforeClass;
import org.junit.Test;

public class GroupBreakTest {

    static GroupBreak br;
    static CatoDataObject obj;

    @BeforeClass
    public static void setUp() {
        obj = TestUtil.createDataObject(1, 2, "a", "b", "c", "d");
        br = new GroupBreak(obj, CatoDataSide.LEFT, TestUtil.ATTR_FIELDS.subList(0, 2), 1);
    }

    @Test
    public void testGetFields() {
        TestUtil.assertEquals(TestUtil.ATTR_FIELDS.subList(0, 2), br.getFields());
    }

    @Test
    public void testGetGroupId() {
        Assert.assertEquals(1, br.getGroupId());
    }
}
