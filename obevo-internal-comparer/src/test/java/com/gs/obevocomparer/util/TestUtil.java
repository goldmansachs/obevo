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
package com.gs.obevocomparer.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import com.gs.obevocomparer.compare.breaks.BreakExclude;
import com.gs.obevocomparer.compare.simple.SimpleCatoProperties;
import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.data.simple.SimpleDataObject;
import com.gs.obevocomparer.data.simple.SimpleDataSchema;
import com.gs.obevocomparer.input.AbstractCatoDataSource;
import com.gs.obevocomparer.input.CatoDataSource;
import com.gs.obevocomparer.output.CatoContentRow.ValueType;
import com.gs.obevocomparer.output.simple.SimpleContentRow;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Ignore;

@Ignore("Only a test utility; no tests to run here")
public class TestUtil {

    public static final List<String> KEY_FIELDS = Arrays.asList("Key 1", "Key 2");
    public static final List<String> ATTR_FIELDS = Arrays.asList("Val 1", "Val 2", "Val 3");
    public static final List<String> EXCLUDE_FIELDS = Arrays.asList("Val 4");
    public static final List<String> ALL_FIELDS = Arrays.asList("Key 1", "Key 2", "Val 1", "Val 2", "Val 3", "Val 4");

    public static SimpleDataSchema createSchema() {
        return new SimpleDataSchema();
    }

    private static SimpleDataSchema createTestSchema() {
        return new SimpleDataSchema();
    }

    public static CatoDataObject createDataObject(Object... fields) {
        if (fields.length < 5 || fields.length > 6) {
            throw new IllegalArgumentException("Must pass 5 or 6 fields to create a test data object");
        }

        CatoDataObject obj = createTestSchema().createDataObject();

        for (int i = 0; i < fields.length; i++) {
            obj.setValue(TestUtil.ALL_FIELDS.get(i), fields[i]);
        }

        return obj;
    }

    public static CatoDataObject createDataObjectWithKeys(Object... fields) {
        CatoDataObject obj = createEmptyDataObject();

        for (int i = 0; i < fields.length; i += 2) {
            obj.setValue(fields[i].toString(), fields[i + 1]);
        }

        return obj;
    }

    public static SimpleDataObject createEmptyDataObject() {
        return createSchema().createDataObject();
    }

    public static SimpleCatoProperties getProperties() {
        return new SimpleCatoProperties(KEY_FIELDS, EXCLUDE_FIELDS);
    }

    public static List<CatoDataObject> getData(CatoDataSource dataSource) {
        List<CatoDataObject> data = new ArrayList<CatoDataObject>();

        if (dataSource instanceof AbstractCatoDataSource) {
            ((AbstractCatoDataSource) dataSource).setDataSchema(createSchema());
        }

        dataSource.open();

        while (dataSource.hasNext()) {
            data.add(dataSource.next());
        }

        dataSource.close();
        return data;
    }

    public static BreakExclude createBreakExclude(String key1, String key2, String field, String leftVal, String rightVal) {
        CatoDataObject key = createEmptyDataObject();
        key.setValue(KEY_FIELDS.get(0), key1);
        key.setValue(KEY_FIELDS.get(1), key2);
        return new BreakExclude(key, field, leftVal, rightVal);
    }

    public static BreakExclude createBreakExclude(String field, String leftVal, String rightVal) {
        return new BreakExclude(null, field, leftVal, rightVal);
    }

    public static SimpleContentRow createContentRow(Object... values) {
        SimpleContentRow row = new SimpleContentRow(values.length);
        for (int i = 0; i < values.length; i++) {
            row.setValue(i, values[i]);
        }
        return row;
    }

    public static SimpleContentRow createContentRow(Object[] values, ValueType[] types) {
        if (values.length != types.length) {
            throw new IllegalArgumentException();
        }

        SimpleContentRow row = new SimpleContentRow(values.length);
        for (int i = 0; i < values.length; i++) {
            row.set(i, values[i], types[i]);
        }
        return row;
    }

    public static void assertEquals(Collection<?> left, Collection<?> right) {
        Assert.assertEquals(right.size(), left.size());

        for (Object obj : right) {
            Assert.assertTrue("Missing object " + obj, left.contains(obj));
        }
    }

    public static void assertLogged(Level level, String message) {
        for (LoggingEvent event : getTestAppender().getEvents()) {
            if (event.getLevel() == level && event.getMessage().toString().contains(message)) {
                return;
            }
        }

        Assert.fail("For level " + level + ", failed to find logged message " + message);
    }

    public static void clearLogged() {
        getTestAppender().getEvents().clear();
    }

    private static TestAppender getTestAppender() {
        return (TestAppender) Logger.getRootLogger().getAppender("TEST");
    }

    public static void assertException(String message, Class<? extends UnsupportedOperationException> clazz, Throwable ex) {
        Assert.assertEquals("Exception Class does not match expected", clazz, ex.getClass());
        Assert.assertTrue("Exception message '" + ex.getMessage() + "' does not contain '" + message + "'",
                ex.getMessage().contains(message));
    }

    public static DataSource createDataSource() {
        // TODO Give an inmemory datasource

        return null;
    }
}
