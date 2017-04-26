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
package com.gs.obevocomparer.compare.breaks;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class BreakExcludeTest {

    CatoDataObject dataKey;
    BreakExclude exclude;

    @Test
    public void testConstructor() {
        this.dataKey = TestUtil.createEmptyDataObject();
        this.dataKey.setValue("Test Key", "Key Value");

        this.exclude = new BreakExclude(this.dataKey, "Field 1");
        Assert.assertSame(this.dataKey, this.exclude.getKey());
        Assert.assertEquals("Field 1", this.exclude.getField());
        assertNull(this.exclude.getLeftValue());
        assertNull(this.exclude.getRightValue());

        this.exclude = new BreakExclude(this.dataKey, "Field 1", 3, 4.5);
        Assert.assertSame(this.dataKey, this.exclude.getKey());
        Assert.assertEquals("Field 1", this.exclude.getField());
        Assert.assertEquals(3, this.exclude.getLeftValue());
        Assert.assertEquals(4.5, this.exclude.getRightValue());

        try {
            new BreakExclude(null, null);
            Assert.fail();
        } catch (Exception ex) {
        }

        try {
            new BreakExclude(null, null, null, null);
            org.junit.Assert.fail();
        } catch (Exception ex) {
        }
    }
}
