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
package com.gs.obevo.db.impl.core.reader;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.platform.ChangeType;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class BaselineTableChangeParserTest {
    @Test
    public void testValue() throws Exception {
        ChangeType tableChangeType = mock(ChangeType.class);
        ChangeType fkChangeType = mock(ChangeType.class);
        ChangeType triggerChangeType = mock(ChangeType.class);
        BaselineTableChangeParser parser = new BaselineTableChangeParser(fkChangeType, triggerChangeType);

        ImmutableList<Change> changes = parser.value(
                tableChangeType, null, "CREATE TABLE TABLE_A (\n" +
                        "\tA_ID    INT\tNOT NULL,\n" +
                        "\tCOL2 INT NULL,\n" +
                        "\tCOL3 INT NULL\n" +
                        ")\n" +
                        "GO\n" +
                        "ALTER TABLE TABLE_A ADD PRIMARY KEY (A_ID)\n" +
                        "GO\n" +
                        "ALTER TABLE TABLE_A ADD FOREIGN KEY FK_B (B_ID) REFERENCES TABLE_B(B_ID)\n" +
                        "GO\n" +
                        "CREATE INDEX TABLE_A_IND ON TABLE_A(COL2, COL3)\n" +
                        "GO\n" +
                        "ALTER TABLE TABLE_A ADD COLUMN COL10 INT NULL\n" +
                        "GO\n", "TABLE_A", "schema", null);
        assertEquals(5, changes.size());

        // ensure that the foreign key is detected and placed at the end, regardless of where it was in the original
        // file
        // the rest of the changes should appear here in order
        assertThat(changes.get(0).getContent(), containsString("CREATE TABLE"));
        assertThat(changes.get(1).getContent(), containsString("PRIMARY KEY"));
        assertThat(changes.get(2).getContent(), containsString("CREATE INDEX"));
        assertThat(changes.get(3).getContent(), containsString("ADD COLUMN"));
        assertThat(changes.get(4).getContent(), containsString("FOREIGN KEY"));
    }
}
