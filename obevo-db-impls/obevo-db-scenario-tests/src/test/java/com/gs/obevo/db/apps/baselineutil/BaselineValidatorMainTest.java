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
package com.gs.obevo.db.apps.baselineutil;

import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.dbmetadata.deepcompare.CompareBreak;
import com.gs.obevo.dbmetadata.deepcompare.FieldCompareBreak;
import com.gs.obevo.dbmetadata.deepcompare.ObjectCompareBreak;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BaselineValidatorMainTest {
    @Test
    public void testBaselineMismatchScenario() {
        DbDeployerAppContext appContext = DbEnvironmentFactory.getInstance().readOneFromSourcePath("baselineutil/BaselineValidatorMain/uc1", "test")
                .buildAppContext();

        BaselineValidatorMain main = new BaselineValidatorMain();
        ImmutableSet<CompareBreak> compareBreaks = main.calculateBaselineBreaks(appContext);

        System.out.println("BREAKS\n" + compareBreaks.makeString("\n"));

        assertEquals(2, compareBreaks.size());
        ObjectCompareBreak objectBreak = (ObjectCompareBreak) compareBreaks.detect(Predicates
                .instanceOf(ObjectCompareBreak.class));

        FieldCompareBreak dataTypeBreak = (FieldCompareBreak) compareBreaks.select(
                Predicates.instanceOf(FieldCompareBreak.class)).detect(new Predicate<CompareBreak>() {
            @Override
            public boolean accept(CompareBreak each) {
                return ((FieldCompareBreak) each).getFieldName().equalsIgnoreCase("columnDataType");
            }
        });

        assertNotNull(objectBreak);
        assertNotNull(dataTypeBreak);
    }
}
