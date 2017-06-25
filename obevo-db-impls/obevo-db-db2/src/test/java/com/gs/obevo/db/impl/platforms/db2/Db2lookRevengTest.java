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
package com.gs.obevo.db.impl.platforms.db2;

import java.io.File;

import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Db2lookRevengTest {
    private static final String SCHEMA = "MYSCHEMA01";

    @Test
    public void testReveng() throws Exception {
        File outputDir = new File("./target/db2revengoutput");
        FileUtils.deleteDirectory(outputDir);

        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema(SCHEMA);
        args.setInputPath(new File("./src/test/resources/reveng/db2look/input/db2input.txt"));
        args.setGenerateBaseline(false);
        args.setOutputPath(outputDir);
        new Db2lookReveng().reveng(args);

        DirectoryAssert.assertDirectoriesEqual(new File("./src/test/resources/reveng/db2look/expected"), new File(outputDir, "final"));
    }

    @Test
    public void testInstructions() throws Exception {
        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema(SCHEMA);
        args.setGenerateBaseline(false);
        args.setOutputPath(new File("./target/db2revenginterm.txt"));

        new Db2lookReveng().reveng(args);
    }

    @Test
    public void testRemoveQuotes() {
        String input = "abc def \t\n\"GHI\" jk \"LMN OP\" \t\nqrs \"tuV\" w \"X_Y\" z\t\n";
        String expected = "abc def \t\nGHI jk \"LMN OP\" \t\nqrs \"tuV\" w X_Y z\t\n";
        assertEquals(expected, Db2lookReveng.removeQuotes(input));
    }

    @Test
    public void testSubstituteTablespace() {
        String input = "create table (abc xyz) IN \"myTablespace\"";
        String expected = "create table (abc xyz) IN \"${myTablespace_token}\"";
        assertEquals("replace the tablespace token if it is found", expected, Db2lookReveng.substituteTablespace(input).getLineOutput());

        String noConvertInput = "create table (abc xyz) in \"myTablespace\"";
        assertEquals("lowercase 'in' should not get picked up for conversion", noConvertInput, Db2lookReveng.substituteTablespace(noConvertInput).getLineOutput());
    }


    @Test
    public void testSchemaExtraction() {
        ImmutableList<AbstractDdlReveng.RevengPattern> patterns = Db2lookReveng.getRevengPatterns().select(Predicates.attributeEqual(AbstractDdlReveng.RevengPattern.TO_CHANGE_TYPE, "VIEW"));

        AbstractDdlReveng.RevengPattern revengPattern = patterns.get(0);
        assertEquals("MYVIEW", revengPattern.evaluate("CREATE or REPLACE VIEW SCHEMA.MYVIEW AS ABC DEF GHI").getPrimaryName());
        assertEquals("MYVIEW", revengPattern.evaluate("CREATE or REPLACE VIEW \"SCHEMA\".\"MYVIEW\" AS ABC DEF GHI").getPrimaryName());
        assertEquals("MYVIEW", revengPattern.evaluate("CREATE or REPLACE VIEW MYVIEW AS ABC DEF GHI").getPrimaryName());
        assertEquals("MYVIEW", revengPattern.evaluate("CREATE or REPLACE VIEW \"MYVIEW\" AS ABC DEF GHI").getPrimaryName());
    }
}
