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
package com.gs.obevo.reladomo;

import java.io.File;

import com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.db.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReladomoSchemaConverterTest {

    @Test
    public void testConvertDdlsToDaFormat() throws Exception {
        final File outputFolder = new File("./target/revengTest");
        FileUtils.deleteQuietly(outputFolder);
        final ReladomoSchemaConverter reladomoSchemaConverter = new ReladomoSchemaConverter();

        final ChangeType tableChangeType = mock(ChangeType.class);
        when(tableChangeType.getName()).thenReturn(ChangeType.TABLE_STR);
        when(tableChangeType.getDirectoryName()).thenReturn("table");

        Platform platform = mock(Platform.class);
        when(platform.getName()).thenReturn("mockPlatform");
        when(platform.getChangeType(ChangeType.TABLE_STR)).thenReturn(tableChangeType);
        when(platform.getObjectExclusionPredicateBuilder()).thenReturn(new ObjectTypeAndNamePredicateBuilder(ObjectTypeAndNamePredicateBuilder.FilterType.EXCLUDE));
        when(platform.convertDbObjectName()).thenReturn(Functions.getStringPassThru());

        reladomoSchemaConverter.convertDdlsToDaFormat(platform, new File("./src/test/resources/reveng/input"),
                outputFolder, "yourSchema", true, null);

        DirectoryAssert.assertDirectoriesEqual(new File("./src/test/resources/reveng/expected"), outputFolder);
    }

    @Test
    public void testSplit() {
        String sql = "alter table APP_INFO_DEPLOYMENT_SERVER add constraint ANPMSRRBC40A8AA_PK ';' primary key (ID);\n\ncreate index AFYSRBC40A8AA_IDX0 on APP_INFO_DEPLOYMENT_SERVER(HOST_NAME);\n;";

        final ListIterable<String> splitStrings = ReladomoSchemaConverter.splitSqlBySemicolon(sql);

        assertEquals(Lists.mutable.with(
                "alter table APP_INFO_DEPLOYMENT_SERVER add constraint ANPMSRRBC40A8AA_PK ';' primary key (ID)",
                "\ncreate index AFYSRBC40A8AA_IDX0 on APP_INFO_DEPLOYMENT_SERVER(HOST_NAME)"
                ), splitStrings);
    }
}