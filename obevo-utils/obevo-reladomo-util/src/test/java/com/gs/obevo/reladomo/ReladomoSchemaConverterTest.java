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
package com.gs.obevo.reladomo;

import java.io.File;

import com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.eclipse.collections.impl.block.factory.Functions;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReladomoSchemaConverterTest {

    @Test
    public void aseConversionExample() throws Exception {
        conversionTest("ase", "SYBASE_ASE");
    }

    @Test
    public void db2ConversionExample() throws Exception {
        conversionTest("db2", "mockPlatform");
    }

    private void conversionTest(String testName, String platformName) {
        final File outputFolder = new File("./target/revengTest/" + testName);
        FileUtils.deleteQuietly(outputFolder);
        final ReladomoSchemaConverter reladomoSchemaConverter = new ReladomoSchemaConverter();

        final ChangeType tableChangeType = mock(ChangeType.class);
        when(tableChangeType.getName()).thenReturn(ChangeType.TABLE_STR);
        when(tableChangeType.getDirectoryName()).thenReturn("table");

        DbPlatform platform = mock(DbPlatform.class);
        when(platform.getName()).thenReturn(platformName);
        when(platform.getChangeType(ChangeType.TABLE_STR)).thenReturn(tableChangeType);
        when(platform.getObjectExclusionPredicateBuilder()).thenReturn(new ObjectTypeAndNamePredicateBuilder(ObjectTypeAndNamePredicateBuilder.FilterType.EXCLUDE));
        when(platform.convertDbObjectName()).thenReturn(Functions.getStringPassThru());

        reladomoSchemaConverter.convertDdlsToDaFormat(platform, new File("./src/test/resources/reveng/input/" + testName),
                outputFolder, "yourSchema", true, null);

        DirectoryAssert.assertDirectoriesEqual(new File("./src/test/resources/reveng/expected/" + testName), new File(outputFolder, "final"));
    }
}
