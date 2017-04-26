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
package com.gs.obevo.db.impl.core.reader;

import java.io.File;

import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.util.FileUtilsCobra;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrepareDbChangeForDbTest {
    @Test
    public void testDefaultTokenMarker() {
        this.testMe("${", "}",
                "src/test/resources/deploy/PrepareDbChangeForDbTest/TableA.1.defaultToken.expected.ddl");
    }

    @Test
    public void testCustomTokenMarker() {
        this.testMe("$", "@", "src/test/resources/deploy/PrepareDbChangeForDbTest/TableA.1.expected.ddl");
    }

    private void testMe(String prefix, String suffix, String compareFile) {
        String schema1 = "schema1";
        String schema2 = "schema2";
        String schema2Changed = "schema2Changed";

        // Setup the db config w/ env _d1
        DbEnvironment env = mock(DbEnvironment.class);
        when(env.getDbSchemaPrefix()).thenReturn("");
        when(env.getDbSchemaSuffix()).thenReturn("_d1");
        when(env.getPhysicalSchemaPrefixInternal(schema1)).thenReturn(schema1);
        when(env.getPhysicalSchemaPrefixInternal(schema2)).thenReturn(schema2Changed);
        when(env.getTokens()).thenReturn(Maps.immutable.with("Param", "is this"));
        when(env.getTokenPrefix()).thenReturn(prefix);
        when(env.getTokenSuffix()).thenReturn(suffix);
        when(env.getSchemas()).thenReturn(Sets.immutable.<Schema>empty());

        // now setup the files
        String filePath = "deploy/PrepareDbChangeForDbTest/TableA.1.ddl";
        File sourcePath = new File("./src/test/resources");

        String content = FileUtilsCobra.readFileToString(new File(sourcePath, filePath));

        PrepareDbChangeForDb processor = new PrepareDbChangeForDb();

        // execute the output
        String output = processor.prepare(content, null, env);

        // verify the result
        File expectedOutputFile = new File(compareFile);
        String expectedContent = FileUtilsCobra.readFileToString(expectedOutputFile);
        assertEquals(expectedContent, output);
    }
}
