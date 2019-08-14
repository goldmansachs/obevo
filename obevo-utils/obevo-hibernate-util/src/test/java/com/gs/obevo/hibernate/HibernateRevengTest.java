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
package com.gs.obevo.hibernate;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.gs.obevo.db.impl.platforms.db2.Db2DbPlatform;
import com.gs.obevo.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.hibernate.dialect.DB2Dialect;
import org.junit.Test;

public abstract class HibernateRevengTest {
    private final File outputPath = new File("./target/hibgentest");

    @Test
    public void testCreation() {
        FileUtils.deleteQuietly(outputPath);

        HibernateReveng<List<? extends Class<?>>> hibReveng = getHibReveng();

        // Reverse-engineer the first default schema
        hibReveng.executeReveng(getRevengArgs("myschema", false));

        // Reverse-engineer the second schema in the model (the ones explicitly marked in the Hibernate class)
        hibReveng.executeReveng(getRevengArgs("mycat", true));

        DirectoryAssert.assertDirectoriesEqual(new File("./src/test/resources/reveng/expected"), new File(outputPath, "final"), true);
    }

    private HibernateRevengArgs<List<? extends Class<?>>> getRevengArgs(String schema, boolean explicitSchemaRequired) {
        return new HibernateRevengArgs(schema, outputPath, new Db2DbPlatform(), DB2Dialect.class, Arrays.asList(HibClassA.class, HibClassB.class, HibClassSchemaC.class, HibClassSchemaD.class))
                .withPostCreateTableSql(" lock datarows")
                .withExplicitSchemaRequired(explicitSchemaRequired);
    }

    protected abstract HibernateReveng<List<? extends Class<?>>> getHibReveng();
}
