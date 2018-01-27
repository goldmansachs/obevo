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
package com.gs.obevocomparer.data;

import com.gs.obevocomparer.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class CatoFieldSelectorTest {

    @Test
    public void testSelector() {
        CatoDataObject obj = TestUtil.createDataObjectWithKeys("Val1", 123, "Val2", 555.5, "Val   3", "abc");

        Assert.assertEquals(123, new CatoFieldSelector("Val1").valueOf(obj));
        org.junit.Assert.assertEquals(555.5, new CatoFieldSelector("Val2").valueOf(obj));
        org.junit.Assert.assertEquals("abc", new CatoFieldSelector("Val   3").valueOf(obj));

        assertNull(new CatoFieldSelector("Not a val").valueOf(obj));
        assertNull(new CatoFieldSelector(null).valueOf(obj));
    }
}
