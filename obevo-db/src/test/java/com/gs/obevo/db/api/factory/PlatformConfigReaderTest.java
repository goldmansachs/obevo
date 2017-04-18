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
package com.gs.obevo.db.api.factory;

import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests around the PlatformConfigReader. Note - no tests yet around having multiple config files of the same name
 * read, that is a todo.
 */
public class PlatformConfigReaderTest {
    private final PlatformConfigReader reader = new PlatformConfigReader();

    @Test
    public void testValidDefault() {
        Properties properties = reader.readPlatformProperties("PlatformConfigReader/validDefault");

        assertEquals(4, properties.size());
        assertEquals("val1", properties.getProperty("prop1"));
        assertEquals("val2", properties.getProperty("prop2"));
        assertEquals("val3", properties.getProperty("key3.prop3"));
        assertEquals("val4", properties.getProperty("key4.prop4"));
    }

    @Test
    public void testValidWithOverride() {
        Properties properties = reader.readPlatformProperties("PlatformConfigReader/validWithOverride");

        assertEquals(4, properties.size());
        assertEquals("val1", properties.getProperty("prop1"));
        assertEquals("val2Override", properties.getProperty("prop2"));
        assertEquals("val3Override", properties.getProperty("key3.prop3"));
        assertEquals("val4", properties.getProperty("key4.prop4"));
    }

    @Test(expected = IllegalStateException.class)
    public void testBadMissingDefault() {
        reader.readPlatformProperties("PlatformConfigReader/badMissingDefault");
    }
}