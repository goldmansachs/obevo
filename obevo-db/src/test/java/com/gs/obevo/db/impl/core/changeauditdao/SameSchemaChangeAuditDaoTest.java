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
package com.gs.obevo.db.impl.core.changeauditdao;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.DeployExecutionDao;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.impl.core.DbDeployerAppContextImpl;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.AbstractSqlExecutor;
import org.apache.commons.dbutils.DbUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.map.MutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests around the {@link ChangeAuditDao} class and verifying that we can upgrade successfully between versions.
 */
@Ignore("This is the abstract class; rely on subclasses for implementation")
public abstract class SameSchemaChangeAuditDaoTest {
    private static final String CHANGETYPE_COL = "CHANGETYPE";

    private final IntToObjectFunction<DbDeployerAppContext> getAppContext;

    public SameSchemaChangeAuditDaoTest(IntToObjectFunction<DbDeployerAppContext> getAppContext) {
        this.getAppContext = getAppContext;
    }

    private String logicalSchema1;
    private String logicalSchema2;
    private PhysicalSchema testSchema;
    private DbPlatform platform;
    private JdbcHelper jdbcHelper;
    private Connection conn;
    private SameSchemaChangeAuditDao artifactDeployerDao;
    private SameSchemaDeployExecutionDao deployExecutionDao;
    private String myVersion = "myVersion";
    private String myVersion2 = "myVersion2";
    
    @Before
    public void setup() throws Exception {
        DbDeployerAppContext context = getAppContext.valueOf(1);

        context.setupEnvInfra();

        this.logicalSchema1 = context.getEnvironment().getSchemas().getFirst().getName();  // pick one from the environment at random as our logical schema so that we can test the retrieval logic
        this.logicalSchema2 = "MYSCHEMA2";  // this is just for testing in this DAO, no need to tie to the actual logical schema
        this.testSchema = context.getEnvironment().getPhysicalSchema(this.logicalSchema1);
        this.platform = context.getEnvironment().getPlatform();
        this.conn = context.getDataSource().getConnection();
        this.jdbcHelper = ((AbstractSqlExecutor) context.getSqlExecutor()).createJdbcHelper(context.getDataSource());
        this.artifactDeployerDao = (SameSchemaChangeAuditDao) ((DbDeployerAppContextImpl) context).getArtifactDeployerDao();
        this.deployExecutionDao = (SameSchemaDeployExecutionDao) context.getDeployExecutionDao();
    }

    @After
    public void teardown() {
        DbUtils.closeQuietly(conn);
    }

    private String getTestPhysicalSchema() {
        return platform.getSchemaPrefix(testSchema);
    }

    @Test
    public void testUpgrade() {
        updateAndIgnoreError("DROP TABLE " + getChangeAuditTableName());
        updateAndIgnoreError("DROP TABLE " + getDeployExecutionTableName());
        updateAndIgnoreError("DROP TABLE " + getDeployExecutionAttrTableName());

        jdbcHelper.update(conn, artifactDeployerDao.get5_0Sql(testSchema));

        deployExecutionDao.init();
        artifactDeployerDao.init();
    }

    private void updateAndIgnoreError(String sql) {
        try {
            jdbcHelper.update(conn, sql);
        } catch (Exception e) {
        }
    }

    private void setup5_3Upgrade() {
        updateAndIgnoreError("DROP TABLE " + getChangeAuditTableName());
        updateAndIgnoreError("DROP TABLE " + getDeployExecutionTableName());
        updateAndIgnoreError("DROP TABLE " + getDeployExecutionAttrTableName());

        jdbcHelper.update(conn, artifactDeployerDao.get5_1Sql(testSchema));

        jdbcHelper.update(conn, deployExecutionDao.get5_2TableSql(testSchema));

        jdbcHelper.update(conn, deployExecutionDao.get5_2AttrTableSql(testSchema));
    }

    @Test
    public void testDeployExecution5_3UpgradeSingleSchema() {
        setup5_3Upgrade();

        // We upgrade a single schema, but declare two versions to upgrade to

        // Execution 1 - goes to schema 1
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionTableName() + " (ID, STATUS, DEPLOYTIME, EXECUTORID, TOOLVERSION, INIT_COMMAND, ROLLBACK_COMMAND)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                1L, "S", new java.sql.Timestamp(new Date().getTime()), "test", "0.0.0", 0, 0
        );
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionAttrTableName() + " (DEPLOYEXECUTIONID, ATTRNAME, ATTRVALUE)" +
                        "VALUES (?, ?, ?)",
                1L, "conduit.version.name", myVersion
        );
        jdbcHelper.update(conn, "INSERT INTO " + getChangeAuditTableName() + " (ARTFTYPE, ARTIFACTPATH, OBJECTNAME, ACTIVE, " + CHANGETYPE_COL + ", UPDATEDEPLOYID, DBSCHEMA)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "R", "n/a", "OBJ1", 1, "VIEW", 1L, logicalSchema1
        );

        // Execution 2 - can't link directly via ARTIFACTDEPLOYMENT, but since it has the same version as schema 1, we can share
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionTableName() + " (ID, STATUS, DEPLOYTIME, EXECUTORID, TOOLVERSION, INIT_COMMAND, ROLLBACK_COMMAND)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                2L, "S", new Timestamp(new Date().getTime()), "test", "0.0.0", 0, 0
        );
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionAttrTableName() + " (DEPLOYEXECUTIONID, ATTRNAME, ATTRVALUE)" +
                        "VALUES (?, ?, ?)",
                2L, "conduit.version.name", myVersion2
        );

        // verify that the new code can still handle a pre-upgraded schema
        ImmutableCollection<DeployExecution> preUpgradeExecutions = deployExecutionDao.getDeployExecutions(logicalSchema1);

        assertThat(preUpgradeExecutions.toList(), hasSize(2));

        deployExecutionDao.init();

        MutableMap<Long, String> schemasById = jdbcHelper.queryForList(conn, "SELECT * FROM " + getDeployExecutionTableName())
                .toMap(toLong("ID"), this.<String>getFunction("DBSCHEMA"));
        MutableMap<Long, String> versionsById = jdbcHelper.queryForList(conn, "SELECT * FROM " + getDeployExecutionTableName())
                .toMap(toLong("ID"), this.<String>getFunction("PRODUCTVERSION"));
        assertEquals(logicalSchema1, schemasById.get(1L));
        assertEquals(logicalSchema1, schemasById.get(2L));
        assertEquals(myVersion, versionsById.get(1L));
        assertEquals(myVersion2, versionsById.get(2L));

        verifyArtifactRetrieval();
    }

    @Test
    public void testDeployExecution5_3UpgradeMultiSchema() {
        setup5_3Upgrade();

        // this test verifies that we can still migrate different logical schemas even if many are found in the same physical schema

        // Exection 1 - belongs to schema1 via ARTIFACTDEPLOYMENT
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionTableName() + " (ID, STATUS, DEPLOYTIME, EXECUTORID, TOOLVERSION, INIT_COMMAND, ROLLBACK_COMMAND)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                1L, "S", new java.sql.Timestamp(new Date().getTime()), "test", "0.0.0", 0, 0
        );
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionAttrTableName() + " (DEPLOYEXECUTIONID, ATTRNAME, ATTRVALUE)" +
                        "VALUES (?, ?, ?)",
                1L, "conduit.version.name", myVersion
        );
        jdbcHelper.update(conn, "INSERT INTO " + getChangeAuditTableName() + " (ARTFTYPE, ARTIFACTPATH, OBJECTNAME, ACTIVE, " + CHANGETYPE_COL + ", UPDATEDEPLOYID, DBSCHEMA)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "R", "n/a", "OBJ1", 1, "VIEW", 1L, logicalSchema1
        );

        // Exection 2 - linked to schema1 via execution 1
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionTableName() + " (ID, STATUS, DEPLOYTIME, EXECUTORID, TOOLVERSION, INIT_COMMAND, ROLLBACK_COMMAND)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                2, "S", new java.sql.Timestamp(new Date().getTime()), "test", "0.0.0", 0, 0
        );
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionAttrTableName() + " (DEPLOYEXECUTIONID, ATTRNAME, ATTRVALUE)" +
                        "VALUES (?, ?, ?)",
                2, "conduit.version.name", myVersion
        );

        // Exection 3 - linked to schema2 via ARTIFACTDEPLOYMENT
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionTableName() + " (ID, STATUS, DEPLOYTIME, EXECUTORID, TOOLVERSION, INIT_COMMAND, ROLLBACK_COMMAND)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                3, "S", new java.sql.Timestamp(new Date().getTime()), "test", "0.0.0", 0, 0
        );
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionAttrTableName() + " (DEPLOYEXECUTIONID, ATTRNAME, ATTRVALUE)" +
                        "VALUES (?, ?, ?)",
                3, "conduit.version.name", myVersion2
        );
        jdbcHelper.update(conn, "INSERT INTO " + getChangeAuditTableName() + " (ARTFTYPE, ARTIFACTPATH, OBJECTNAME, ACTIVE, " + CHANGETYPE_COL + ", UPDATEDEPLOYID, DBSCHEMA)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "R", "n/a", "OBJ2", 1, "VIEW", 3L, logicalSchema2
        );

        deployExecutionDao.init();

        MutableMap<Long, String> schemasById = jdbcHelper.queryForList(conn, "SELECT * FROM " + getDeployExecutionTableName())
                .toMap(toLong("ID"), this.<String>getFunction("DBSCHEMA"));
        MutableMap<Long, String> versionsById = jdbcHelper.queryForList(conn, "SELECT * FROM " + getDeployExecutionTableName())
                .toMap(toLong("ID"), this.<String>getFunction("PRODUCTVERSION"));
        assertEquals(logicalSchema1, schemasById.get(1L));
        assertEquals(logicalSchema1, schemasById.get(2L));
        assertEquals(logicalSchema2, schemasById.get(3L));
        assertEquals(myVersion, versionsById.get(1L));
        assertEquals(myVersion, versionsById.get(2L));
        assertEquals(myVersion2, versionsById.get(3L));

        verifyArtifactRetrieval();
    }

    private void verifyArtifactRetrieval() {
        assertThat(artifactDeployerDao.getDeployedChanges().toList(), hasSize(greaterThan(0)));
        assertThat(deployExecutionDao.getDeployExecutions(logicalSchema1).toList(), hasSize(greaterThan(0)));
    }

    @Test
    public void testDeployExecution5_3UpgradeMultiSchemaInvalidCase() {
        setup5_3Upgrade();

        // execution 1 - normal use case, on version1
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionTableName() + " (ID, STATUS, DEPLOYTIME, EXECUTORID, TOOLVERSION, INIT_COMMAND, ROLLBACK_COMMAND)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                1L, "S", new java.sql.Timestamp(new Date().getTime()), "test", "0.0.0", 0, 0
        );
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionAttrTableName() + " (DEPLOYEXECUTIONID, ATTRNAME, ATTRVALUE)" +
                        "VALUES (?, ?, ?)",
                1L, "conduit.version.name", myVersion
        );
        jdbcHelper.update(conn, "INSERT INTO " + getChangeAuditTableName() + " (ARTFTYPE, ARTIFACTPATH, OBJECTNAME, ACTIVE, " + CHANGETYPE_COL + ", UPDATEDEPLOYID, DBSCHEMA)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "R", "n/a", "OBJ1", 1, "VIEW", 1L, logicalSchema1
        );

        // execution 2 - no schema is defined, and we have no linkages to any artifacts. Thus, we end up defaulting to the "NOSCHEMA" value
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionTableName() + " (ID, STATUS, DEPLOYTIME, EXECUTORID, TOOLVERSION, INIT_COMMAND, ROLLBACK_COMMAND)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                2L, "S", new java.sql.Timestamp(new Date().getTime()), "test", "0.0.0", 0, 0
        );
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionAttrTableName() + " (DEPLOYEXECUTIONID, ATTRNAME, ATTRVALUE)" +
                        "VALUES (?, ?, ?)",
                2L, "conduit.version.name", myVersion
        );

        // execution 3 - on version 2
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionTableName() + " (ID, STATUS, DEPLOYTIME, EXECUTORID, TOOLVERSION, INIT_COMMAND, ROLLBACK_COMMAND)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                3L, "S", new java.sql.Timestamp(new Date().getTime()), "test", "0.0.0", 0, 0
        );
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionAttrTableName() + " (DEPLOYEXECUTIONID, ATTRNAME, ATTRVALUE)" +
                        "VALUES (?, ?, ?)",
                3L, "conduit.version.name", myVersion
        );
        jdbcHelper.update(conn, "INSERT INTO " + getChangeAuditTableName() + " (ARTFTYPE, ARTIFACTPATH, OBJECTNAME, ACTIVE, " + CHANGETYPE_COL + ", UPDATEDEPLOYID, DBSCHEMA)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "R", "n/a", "OBJ2", 1, "VIEW", 3L, logicalSchema2
        );

        // execution 4 - multiple schemas associated to it in ARTIFACTDEPLOYMENT. This shouldn't ever happen, but we code for it just in case
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionTableName() + " (ID, STATUS, DEPLOYTIME, EXECUTORID, TOOLVERSION, INIT_COMMAND, ROLLBACK_COMMAND)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                4L, "S", new java.sql.Timestamp(new Date().getTime()), "test", "0.0.0", 0, 0
        );
        jdbcHelper.update(conn, "INSERT INTO " + getDeployExecutionAttrTableName() + " (DEPLOYEXECUTIONID, ATTRNAME, ATTRVALUE)" +
                        "VALUES (?, ?, ?)",
                4L, "conduit.version.name", myVersion
        );
        jdbcHelper.update(conn, "INSERT INTO " + getChangeAuditTableName() + " (ARTFTYPE, ARTIFACTPATH, OBJECTNAME, ACTIVE, " + CHANGETYPE_COL + ", UPDATEDEPLOYID, DBSCHEMA)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "R", "n/a", "OBJ3", 1, "VIEW", 4L, logicalSchema1
        );
        jdbcHelper.update(conn, "INSERT INTO " + getChangeAuditTableName() + " (ARTFTYPE, ARTIFACTPATH, OBJECTNAME, ACTIVE, " + CHANGETYPE_COL + ", UPDATEDEPLOYID, DBSCHEMA)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "R", "n/a", "OBJ4", 1, "VIEW", 4L, logicalSchema2
        );

        deployExecutionDao.init();

        MutableMap<Long, String> schemasById = jdbcHelper.queryForList(conn, "SELECT * FROM " + getDeployExecutionTableName())
                .toMap(toLong("ID"), this.<String>getFunction("DBSCHEMA"));
        MutableMap<Long, String> versionsById = jdbcHelper.queryForList(conn, "SELECT * FROM " + getDeployExecutionTableName())
                .toMap(toLong("ID"), this.<String>getFunction("PRODUCTVERSION"));
        assertEquals(logicalSchema1, schemasById.get(1L));
        assertEquals("NOSCHEMA", schemasById.get(2L));
        assertEquals(logicalSchema2, schemasById.get(3L));
        assertEquals("MULTISCHEMA", schemasById.get(4L));
        assertEquals(myVersion, versionsById.get(1L));
        assertEquals(myVersion, versionsById.get(2L));
        assertEquals(myVersion, versionsById.get(3L));
        assertEquals(myVersion, versionsById.get(4L));
    }

    private String getDeployExecutionAttrTableName() {
        return getTestPhysicalSchema() + platform.convertDbObjectName().valueOf(DeployExecutionDao.DEPLOY_EXECUTION_ATTRIBUTE_TABLE_NAME);
    }

    private String getChangeAuditTableName() {
        return getTestPhysicalSchema() + platform.convertDbObjectName().valueOf(ChangeAuditDao.CHANGE_AUDIT_TABLE_NAME);
    }

    private String getDeployExecutionTableName() {
        return getTestPhysicalSchema() + platform.convertDbObjectName().valueOf(DeployExecutionDao.DEPLOY_EXECUTION_TABLE_NAME);
    }

    private <T> Function<Map<String, Object>, T> getFunction(final String field) {
        return stringObjectMap -> (T) stringObjectMap.get(field);
    }

    private Function<Map<String, Object>, Long> toLong(final String field) {
        return stringObjectMap -> platform.getLongValue(stringObjectMap.get(field));
    }
}
