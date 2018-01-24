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

import com.gs.obevo.api.appdata.DeploySystem;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.api.factory.XmlFileConfigReader;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.api.appdata.Group;
import com.gs.obevo.db.api.appdata.Permission;
import com.gs.obevo.db.api.appdata.User;
import com.gs.obevo.util.vfs.FileObject;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DbEnvironmentXmlEnricherTest {
    private final DbEnvironmentXmlEnricher enricher = new DbEnvironmentXmlEnricher();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void test1() {
        // ensure that we can read the default system-config.xml from the folder
        DeploySystem system = getDeploySystem("./src/test/resources/DbEnvironmentXmlEnricher");

        MutableList<DbEnvironment> envs = system.getEnvironments().toList().sortThisBy(Environment.TO_NAME);
        DbEnvironment env1 = envs.detect(Predicates.attributeEqual(Environment.TO_NAME, "test1"));
        DbEnvironment env2 = envs.detect(Predicates.attributeEqual(Environment.TO_NAME, "test2"));
        DbEnvironment env3 = envs.detect(Predicates.attributeEqual(Environment.TO_NAME, "test3"));
        DbEnvironment env4 = envs.detect(Predicates.attributeEqual(Environment.TO_NAME, "test4"));

        Schema schema1 = env1.getSchemas().detect(Predicates.attributeEqual(
                Schema.TO_NAME, "SCHEMA1"));

        MutableSetMultimap<String, String> expectedExclusions = Multimaps.mutable.set.empty();
        expectedExclusions.putAll(ChangeType.TABLE_STR, Lists.immutable.with("tab1", "tab2"));
        expectedExclusions.putAll(ChangeType.VIEW_STR, Lists.immutable.with("view1Pat", "view2%Pat"));
        expectedExclusions.putAll(ChangeType.SP_STR, Lists.immutable.with("sp1", "sp2", "sp3", "sp4"));
        assertEquals(expectedExclusions, schema1.getObjectExclusionPredicateBuilder().getObjectNamesByType());

        assertEquals(Sets.mutable.with("grp1", "grp2", "DACT_RO", "DACT_RO_BATCH1", "DACT_RO_BATCH2", "DACT_RW", "DACT_RW_BATCH1", "DACT_RW_BATCH2", "detokenizedProcGroup", "${myOtherGroupNoToken}"), env1.getGroups().collect(Group.TO_NAME).toSet());
        assertEquals(Sets.mutable.with("usr1", "usr2", "CMDRRODB", "CMDRRWDB"), env1.getUsers().collect(User.TO_NAME).toSet());
        User user1 = env1.getUsers().detect(Predicates.attributeEqual(User.TO_NAME, "usr1"));
        User user2 = env1.getUsers().detect(Predicates.attributeEqual(User.TO_NAME, "usr2"));
        assertEquals("usr1", user1.getName());
        assertNull(user1.getPassword());
        assertFalse(user1.isAdmin());
        assertEquals("usr2", user2.getName());
        assertEquals("pass", user2.getPassword());
        assertTrue(user2.isAdmin());

        assertEquals(DbEnvironmentXmlEnricherTest1DbPlatform.class, env1.getPlatform().getClass());
        assertFalse(env1.isAutoReorgEnabled());
        assertEquals('!', env1.getDataDelimiter());
        assertEquals("nulTok", env1.getNullToken());

        assertEquals(3, env1.getPermissions().size());
        Permission tablePerm = env1.getPermissions().detect(Predicates.attributeEqual(Permission.TO_SCHEME, "TABLE"));
        assertEquals(2, tablePerm.getGrants().size());

        assertEquals(Sets.immutable.with("DACT_RO_BATCH1", "DACT_RO_BATCH2", "DACT_RO"),
                tablePerm.getGrants().get(0).getGrantTargets().get(GrantTargetType.GROUP).toSet());
        assertEquals(Sets.immutable.with("CMDRRODB"), tablePerm.getGrants().get(0).getGrantTargets().get(GrantTargetType.USER)
                .toSet());
        assertEquals(Sets.immutable.with("SELECT"), tablePerm.getGrants().get(0).getPrivileges().toSet());

        assertEquals(Sets.immutable.with("DACT_RW_BATCH1", "DACT_RW_BATCH2", "DACT_RW"),
                tablePerm.getGrants().get(1).getGrantTargets().get(GrantTargetType.GROUP).toSet());
        assertEquals(Sets.immutable.with("CMDRRWDB"), tablePerm.getGrants().get(1).getGrantTargets().get(GrantTargetType.USER)
                .toSet());
        assertEquals(Sets.immutable.with("SELECT", "INSERT", "UPDATE", "DELETE", "INDEX", "REFERENCES"),
                tablePerm.getGrants().get(1).getPrivileges().toSet());

        // demonstrate the override capability
        assertEquals(1, env2.getPermissions().size());

        assertEquals(Sets.mutable.with("SCHEMA1", "SCHEMA2"), env1.getSchemaNames().toSet());
        assertEquals(Sets.mutable.with("SCHEMA1", "SCHEMA2"), env1.getAllSchemas().collect(Schema.TO_NAME).toSet());
        assertEquals(Sets.mutable.with("SCHEMA3"), env2.getSchemaNames().toSet());
        assertEquals(Sets.mutable.with("SCHEMA3", "SCHEMA4_RO"), env2.getAllSchemas().collect(Schema.TO_NAME).toSet());
        assertEquals(Sets.mutable.with("SCHEMA1", "SCHEMA2", "SCHEMA3"), env3.getSchemaNames().toSet());
        assertEquals(Sets.mutable.with("SCHEMA1", "SCHEMA2", "SCHEMA3", "SCHEMA4_RO"), env3.getAllSchemas().collect(Schema.TO_NAME).toSet());

        assertTrue(env2.getAllSchemas().detect(Predicates.attributeEqual(Schema.TO_NAME, "SCHEMA4_RO")).isReadOnly());
        assertFalse(env2.getAllSchemas().detect(Predicates.attributeEqual(Schema.TO_NAME, "SCHEMA3")).isReadOnly());
        assertTrue(env3.getAllSchemas().detect(Predicates.attributeEqual(Schema.TO_NAME, "SCHEMA4_RO")).isReadOnly());
        assertFalse(env3.getAllSchemas().detect(Predicates.attributeEqual(Schema.TO_NAME, "SCHEMA3")).isReadOnly());

        assertEquals("overriden_SCHEMA1", env1.getPhysicalSchema("SCHEMA1").getPhysicalName());
        assertEquals("SCHEMA4_RO", env2.getPhysicalSchema("SCHEMA4_RO").getPhysicalName());
        assertEquals("SCHEMA1_MYSUFFIX", env3.getPhysicalSchema("SCHEMA1").getPhysicalName());
        assertEquals("overriden_SCHEMA4_RO", env3.getPhysicalSchema("SCHEMA4_RO").getPhysicalName());
        assertEquals("MYPREFIX_SCHEMA1", env4.getPhysicalSchema("SCHEMA1").getPhysicalName());
        assertEquals("MYPREFIX_SCHEMA4_RO", env4.getPhysicalSchema("SCHEMA4_RO").getPhysicalName());

        assertEquals(Sets.mutable.with("overriden_SCHEMA1", "prefSCHEMA2suff"), env1.getPhysicalSchemas().collect(PhysicalSchema.TO_PHYSICAL_NAME));
        assertEquals(Sets.mutable.with("overriden_SCHEMA1"), env2.getPhysicalSchemas().collect(PhysicalSchema.TO_PHYSICAL_NAME));
        assertEquals(Sets.mutable.with("SCHEMA1_MYSUFFIX", "SCHEMA2_MYSUFFIX", "SCHEMA3_MYSUFFIX"), env3.getPhysicalSchemas().collect(PhysicalSchema.TO_PHYSICAL_NAME));
        assertEquals(Sets.mutable.with("MYPREFIX_SCHEMA1", "MYPREFIX_SCHEMA2", "MYPREFIX_SCHEMA3"), env4.getPhysicalSchemas().collect(PhysicalSchema.TO_PHYSICAL_NAME));
        assertEquals(Sets.mutable.with("overriden_SCHEMA1", "prefSCHEMA2suff"), env1.getAllPhysicalSchemas().collect(PhysicalSchema.TO_PHYSICAL_NAME));
        assertEquals(Sets.mutable.with("overriden_SCHEMA1", "SCHEMA4_RO"), env2.getAllPhysicalSchemas().collect(PhysicalSchema.TO_PHYSICAL_NAME));
        assertEquals(Sets.mutable.with("SCHEMA1_MYSUFFIX", "SCHEMA2_MYSUFFIX", "SCHEMA3_MYSUFFIX", "overriden_SCHEMA4_RO"), env3.getAllPhysicalSchemas().collect(PhysicalSchema.TO_PHYSICAL_NAME));
        assertEquals(Sets.mutable.with("MYPREFIX_SCHEMA1", "MYPREFIX_SCHEMA2", "MYPREFIX_SCHEMA3", "MYPREFIX_SCHEMA4_RO"), env4.getAllPhysicalSchemas().collect(PhysicalSchema.TO_PHYSICAL_NAME));

        assertEquals(Lists.mutable.of(
                FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("src/test/resources/DbEnvironmentXmlEnricher")
                , FileRetrievalMode.CLASSPATH.resolveSingleFileObject("DbEnvironmentXmlEnricher/extradir1")
                , FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("src/test/resources/DbEnvironmentXmlEnricher/extradir2")
        ), env1.getSourceDirs());
        assertTrue(env1.isCleanBuildAllowed());
        assertEquals("defId", env1.getDefaultUserId());
        assertEquals("defPass", env1.getDefaultPassword());
        assertEquals("host", env1.getDbHost());
        assertEquals(123, env1.getDbPort());
        assertEquals("dbServ", env1.getDbServer());
        assertEquals("dbSrc", env1.getDbDataSourceName());
        assertEquals("pref", env1.getDbSchemaPrefix());
        assertEquals("suff", env1.getDbSchemaSuffix());
        assertEquals("url", env1.getJdbcUrl());
        assertTrue(env1.isPersistToFile());
        assertTrue(env1.isDisableAuditTracking());
        assertEquals("defTab", env1.getDefaultTablespace());
        assertTrue(env1.isChecksumDetectionEnabled());

        assertEquals("val", env1.getTokens().get("key"));
        assertEquals("val2", env1.getTokens().get("key2"));

        assertEquals(Lists.mutable.of(
                FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("src/test/resources/DbEnvironmentXmlEnricher")
                , FileRetrievalMode.CLASSPATH.resolveSingleFileObject("DbEnvironmentXmlEnricher/extradir1")
                , FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("src/test/resources/DbEnvironmentXmlEnricher/extradir2")
        ), env2.getSourceDirs());
        assertFalse(env2.isCleanBuildAllowed());
        assertNull(env2.getDefaultUserId());
        assertNull(env2.getDefaultPassword());
        assertNull(env2.getDbHost());
        assertEquals(0, env2.getDbPort());
        assertNull(env2.getDbServer());
        assertNull(env2.getDbDataSourceName());
        assertEquals("", env2.getDbSchemaPrefix());
        assertEquals("", env2.getDbSchemaSuffix());
        assertNull(env2.getJdbcUrl());
        assertFalse(env2.isPersistToFile());
        assertFalse(env2.isDisableAuditTracking());
        assertNull(env2.getDefaultTablespace());
        assertFalse(env2.isChecksumDetectionEnabled());

        assertNull(env2.getTokens().get("key"));
        assertNull(env2.getTokens().get("key2"));

        assertEquals("env specific\n\n            sql", env1.getAuditTableSql());
        assertEquals("global\nsql", env2.getAuditTableSql());

        assertEquals(Sets.immutable.with("overridenExt1", "overridenExt2", "overridenExt3"), env1.getAcceptedExtensions());
        assertEquals(Sets.immutable.with("overridenExt1", "overridenExt2", "overridenExt3"), env2.getAcceptedExtensions());

        assertTrue(env1.isInvalidObjectCheckEnabled());
        assertTrue(env1.isReorgCheckEnabled());
        assertFalse(env2.isInvalidObjectCheckEnabled());
        assertFalse(env2.isReorgCheckEnabled());

    }

    /**
     * This one has no default specified for autoReorgEnabled
     */
    @Test
    public void test2() {
        DeploySystem system = getDeploySystem("./src/test/resources/DbEnvironmentXmlEnricher/system-config-test2.xml");

        MutableList<DbEnvironment> envs = system.getEnvironments().toList().sortThisBy(Environment.TO_NAME);
        DbEnvironment env1 = envs.detect(Predicates.attributeEqual(Environment.TO_NAME, "test1"));

        Schema schema = env1.getSchemas().detect(Predicates.attributeEqual(
                Schema.TO_NAME, "DEPLOY_TRACKER"));
        assertTrue(schema.getObjectExclusionPredicateBuilder().getObjectNamesByType().isEmpty());
        assertEquals("DEPLOY_TRACKER", env1.getPhysicalSchema("DEPLOY_TRACKER").getPhysicalName());

        assertEquals(Lists.mutable.of(
                FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("src/test/resources/DbEnvironmentXmlEnricher")
        ), env1.getSourceDirs());

        assertEquals(Sets.mutable.with("DACT_RO", "DACT_RO_BATCH1", "DACT_RO_BATCH2", "DACT_RW", "DACT_RW_BATCH1", "DACT_RW_BATCH2"), env1.getGroups().collect(Group.TO_NAME).toSet());
        assertEquals(Sets.mutable.with("CMDRRWDB", "CMDRRODB"), env1.getUsers().collect(User.TO_NAME).toSet());

        assertEquals(DbEnvironmentXmlEnricherTest2DbPlatform.class, env1.getPlatform().getClass());
        assertTrue(env1.isAutoReorgEnabled());
        assertEquals(',', env1.getDataDelimiter());
        assertEquals("null", env1.getNullToken());
        assertEquals(1, env1.getSourceDirs().size());

        assertEquals(Sets.immutable.with("ext1", "ext2"), env1.getAcceptedExtensions());
    }

    @Test
    public void badFileDupeEnvs() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("duplicate env names"));

        getDeploySystem("./src/test/resources/DbEnvironmentXmlEnricher/system-config-bad-dupeEnvNames.xml");
    }

    @Test
    public void badFileDeprecatedExclusionTypes() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(containsString("Pattern element is deprecated"));

        getDeploySystem("./src/test/resources/DbEnvironmentXmlEnricher/system-config-bad-deprecatedExclusionTypes.xml");
    }

    private DeploySystem getDeploySystem(String pathStr) {
        FileObject sourcePath = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject(pathStr);
        HierarchicalConfiguration config = new XmlFileConfigReader().getConfig(sourcePath);
        return enricher.readSystem(config, sourcePath);
    }
}
