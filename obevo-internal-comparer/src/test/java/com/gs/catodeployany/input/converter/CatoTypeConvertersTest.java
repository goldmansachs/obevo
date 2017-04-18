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
package com.gs.catodeployany.input.converter;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.gs.catodeployany.input.CatoTypeConverter;
import org.eclipse.collections.api.block.function.Function;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class CatoTypeConvertersTest {

    private CatoTypeConverter converter = CatoTypeConverters.newPreciseDoubleStringType(2, "MM/dd/yyyy HH:mm:ss",
            "MM/dd/yyyy");

    @Test
    public void trivialConvertTest() {
        assertNull(this.converter.convert(null));
        this.assertConvert(Boolean.class, true, true);

        Object obj = new Object();
        this.assertConvert(Object.class, obj, obj);

        this.assertConvert(String.class, "abc", "abc");
        this.assertConvert(String.class, "true2", "true2");
        assertNull(this.converter.convert(""));
        assertNull(this.converter.convert("   ")); // spaces
        assertNull(this.converter.convert("	")); // tab
    }

    @Test
    public void booleanConvertTest() {
        this.assertConvert(Boolean.class, true, "true");
        this.assertConvert(Boolean.class, true, "   true      ");
        this.assertConvert(Boolean.class, true, "TRUE");
        this.assertConvert(Boolean.class, true, "tRuE");
        this.assertConvert(Boolean.class, false, "false");
        this.assertConvert(Boolean.class, false, "FALSE");
        this.assertConvert(Boolean.class, false, "fAlSe");
    }

    @Test
    public void integerConvertTest() {
        this.assertConvert(Integer.class, 123, "123");
        this.assertConvert(Integer.class, 4567, "  4567   ");
        this.assertConvert(Integer.class, -234, "-234");
        this.assertConvert(Integer.class, 455524567, "455524567");
        this.assertConvert(Long.class, 4555245679L, "4555245679");
        this.assertConvert(Long.class, 445566887445L, "445566887445");
        this.assertConvert(Long.class, -12345678910L, "-12345678910");
        this.assertConvert(Integer.class, 12, "012");
    }

    @Test
    public void doubleConvertTest() {
        this.assertConvert(Double.class, 4.5, "4.5");
        this.assertConvert(Double.class, 4.5, "  4.5  ");
        this.assertConvert(Double.class, 2.0, "2.0");
        this.assertConvert(Double.class, 39.5, "39.499");
        this.assertConvert(Double.class, 0.46, ".4556551");
        this.assertConvert(Double.class, 3.0, "3.");
        this.assertConvert(Double.class, 3.0, "+3.0");
        this.assertConvert(Double.class, 3.0, "+3");
        this.assertConvert(Double.class, -3.0, "-3.0");
        this.assertConvert(Double.class, 123456789123456789123456789123456789123456789123456789.0,
                "123456789123456789123456789123456789123456789123456789");

        this.assertConvert(String.class, "234E5", "234E5");
        this.assertConvert(String.class, "123Q5", "123Q5");
        this.assertConvert(String.class, "2.4E5", "2.4E5");
        this.assertConvert(String.class, "-3.45E6", "-3.45E6");
    }

    @Test
    public void dateConvertTest() {

        this.converter = new StringTypeConverter();

        this.assertConvert(String.class, "05/12/2010", "05/12/2010");
        this.assertConvert(String.class, "05/12/2010 15:05:05", "05/12/2010 15:05:05");

        this.converter = new StringTypeConverter("MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy");

        this.assertConvert(Date.class, new GregorianCalendar(2010, Calendar.MAY, 12).getTime(), "05/12/2010");
        this.assertConvert(Date.class, new GregorianCalendar(2010, Calendar.MAY, 12, 15, 5, 5).getTime(),
                "05/12/2010 15:05:05");
        this.assertConvert(String.class, "2010/05/12", "2010/05/12");
    }

    @Test
    public void testCustomConversion() {

        Function converter = new Function() {

            @Override
            public Object valueOf(Object arg0) {
                return (String) arg0 + "-custom";
            }
        };

        org.junit.Assert.assertEquals("string-custom", CatoTypeConverters.newCustomTypeConverter(converter).convert("string"));
    }

    private void assertConvert(Class<?> expectedClass, Object expectedObj, Object obj) {
        Object convertedObj = this.converter.convert(obj);
        org.junit.Assert.assertEquals(expectedClass, convertedObj.getClass());
        org.junit.Assert.assertEquals(expectedObj, convertedObj);
    }
}
