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
package com.gs.obevocomparer.compare.simple;

import com.gs.obevocomparer.compare.CatoDataComparator;
import com.gs.obevocomparer.util.TestUtil;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.Assert;
import org.junit.Test;

public class SimpleCatoPropertiesTest {

    final SimpleCatoProperties fields = new SimpleCatoProperties(Lists.mutable.with("A", "B"), Lists.mutable.with("X",
            "Y"));

    @Test
    public void testMappedFields() {

        Assert.assertEquals(0, this.fields.getMappedFields().size());

        this.fields.addMappedField("C", "CCC");
        this.fields.addMappedField("D", "DDD");

        Assert.assertEquals(2, this.fields.getMappedFields().size());
        Assert.assertEquals("CCC", this.fields.getMappedFields().get("C"));
        Assert.assertEquals("DDD", this.fields.getMappedFields().get("D"));

        this.fields.setMappedFields("E", "EEE", "F", "FFF", "G", "GGG");

        Assert.assertEquals(3, this.fields.getMappedFields().size());
        Assert.assertEquals("EEE", this.fields.getMappedFields().get("E"));
        Assert.assertEquals("FFF", this.fields.getMappedFields().get("F"));
        Assert.assertEquals("GGG", this.fields.getMappedFields().get("G"));

        try {
            this.fields.setMappedFields("Not", "going to", "work");
            Assert.fail("Mapped field set should have failed");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("Must pass an even number of fields to map", ex.getMessage());
        }
    }

    @Test
    public void testBreakExcludes() {
        Assert.assertEquals(0, this.fields.getBreakExcludes().size());

        this.fields.withBreakExcludes(Lists.mutable.with(TestUtil.createBreakExclude("A", "B", "C"),
                TestUtil.createBreakExclude("D", "E", "F")));

        Assert.assertEquals(2, this.fields.getBreakExcludes().size());
    }

    @Test
    public void testDecimalPrecision() {

        Assert.assertEquals(CatoDataComparator.DEFAULT_DECIMAL_PRECISION, this.fields.getDecimalPrecision());

        this.fields.withDecimalPrecision(2);

        Assert.assertEquals(2, this.fields.getDecimalPrecision());
    }
}
