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
import java.util.Map;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.api.appdata.ChangeRerunnable;
import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.appdata.DeployExecutionAttribute;
import com.gs.obevo.api.appdata.DeployExecutionImpl;
import com.gs.obevo.api.appdata.DeployExecutionStatus;
import com.gs.obevo.api.appdata.ObjectKey;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployExecutionDao;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Grant;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.api.appdata.Permission;
import com.gs.obevo.db.api.platform.DbChangeTypeBehavior;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry;
import com.gs.obevo.util.VisibleForTesting;
import com.gs.obevo.util.knex.InternMap;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.function.checked.ThrowingFunction;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AuditDao that will write the audit to the same db schema as the Change audit
 */
public class SameSchemaChangeAuditDao implements ChangeAuditDao {
    private static final Logger LOG = LoggerFactory.getLogger(SameSchemaChangeAuditDao.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final SqlExecutor sqlExecutor;
    private final DbEnvironment env;
    private final DbMetadataManager dbMetadataManager;
    private final String deployUserId;
    private final SameSchemaDeployExecutionDao deployExecutionDao;
    private final ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry;

    private final String dbChangeTable;
    private final String changeNameColumn;
    private final String changeTypeColumn;
    private final String deployUserIdColumn;
    private final String timeInsertedColumn;
    private final String timeUpdatedColumn;
    private final String rollbackContentColumn;
    private final String insertDeployExecutionIdColumn;
    private final String updateDeployExecutionIdColumn;

    // older version of static data
    private static final String OLD_STATICDATA_CHANGETYPE = "METADATA";

    public SameSchemaChangeAuditDao(DbEnvironment env, SqlExecutor sqlExecutor, DbMetadataManager dbMetadataManager,
            String deployUserId, DeployExecutionDao deployExecutionDao, ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry) {
        this.sqlExecutor = sqlExecutor;
        this.env = env;
        this.dbMetadataManager = dbMetadataManager;
        this.deployUserId = deployUserId;
        this.deployExecutionDao = (SameSchemaDeployExecutionDao) deployExecutionDao;
        this.changeTypeBehaviorRegistry = changeTypeBehaviorRegistry;

        Function<String, String> convertDbObjectName = env.getPlatform().convertDbObjectName();
        this.dbChangeTable = convertDbObjectName.valueOf(CHANGE_AUDIT_TABLE_NAME);  // for backwards-compatibility, the dbChange table is named "ARTIFACTDEPLOYMENT". We hope to migrate existing tables eventually
        this.changeNameColumn = convertDbObjectName.valueOf("ARTIFACTPATH");  // for backwards-compatibility, the changeName column is named "ArtifactPath". We hope to migrate existing tables eventually
        this.changeTypeColumn = convertDbObjectName.valueOf("CHANGETYPE");
        this.deployUserIdColumn = convertDbObjectName.valueOf("DEPLOY_USER_ID");
        this.timeInsertedColumn = convertDbObjectName.valueOf("TIME_INSERTED");
        this.timeUpdatedColumn = convertDbObjectName.valueOf("TIME_UPDATED");
        this.rollbackContentColumn = convertDbObjectName.valueOf("ROLLBACKCONTENT");
        this.insertDeployExecutionIdColumn = convertDbObjectName.valueOf("INSERTDEPLOYID");
        this.updateDeployExecutionIdColumn = convertDbObjectName.valueOf("UPDATEDEPLOYID");
    }

    @Override
    public String getAuditContainerName() {
        return dbChangeTable;
    }

    @Override
    public void init() {
        for (final String schema : env.getSchemaNames()) {
            final PhysicalSchema physicalSchema = env.getPhysicalSchema(schema);
            sqlExecutor.executeWithinContext(physicalSchema, new Procedure<Connection>() {
                @Override
                public void value(Connection conn) {
                    init(conn, schema);
                }
            });
        }
    }

    private void init(Connection conn, String schema) {
        PhysicalSchema physicalSchema = env.getPhysicalSchema(schema);
        DaTable artifactTable = this.dbMetadataManager.getTableInfo(physicalSchema, dbChangeTable, new DaSchemaInfoLevel().setRetrieveTableColumns(true));
        JdbcHelper jdbc = sqlExecutor.getJdbcTemplate();
        if (artifactTable == null) {
            String auditTableSql = get5_1Sql(physicalSchema);
            jdbc.execute(conn, auditTableSql);

            // We only grant SELECT access to PUBLIC so that folks can read the audit table without DBO permission
            // (we don't grant R/W access, as we assume whatever login that executes deployments already has DBO access,
            // and so can modify this table)
            DbChangeTypeBehavior tableChangeType = (DbChangeTypeBehavior) changeTypeBehaviorRegistry.getChangeTypeBehavior(ChangeType.TABLE_STR);

            tableChangeType.applyGrants(conn, physicalSchema, dbChangeTable, Lists.immutable.with(new Permission("artifacTable",
                    Lists.immutable.with(new Grant(Lists.immutable.with("SELECT"), Multimaps.immutable.list.with(GrantTargetType.PUBLIC, "PUBLIC"))))));
        } else {
            // We will still grant this here to make up for the existing DBs that did not have the grants given
            DbChangeTypeBehavior tableChangeType = (DbChangeTypeBehavior) changeTypeBehaviorRegistry.getChangeTypeBehavior(ChangeType.TABLE_STR);

            String schemaPlusTable = env.getPlatform().getSubschemaPrefix(physicalSchema) + dbChangeTable;

            // Here, we detect if we are on an older version of the table due to missing columns (added for version
            // 3.9.0). If we find the
            // columns are missing, we will add them and backfill
            if (artifactTable.getColumn(deployUserIdColumn) == null) {
                jdbc.execute(conn, String.format("alter table %s ADD %s %s %s", schemaPlusTable,
                        deployUserIdColumn, "VARCHAR(32)", env.getPlatform().getNullMarkerForCreateTable()));
                jdbc.execute(conn, String.format("UPDATE %s SET %s = %s", schemaPlusTable,
                        deployUserIdColumn, "'backfill'"));
            }
            if (artifactTable.getColumn(timeUpdatedColumn) == null) {
                jdbc.execute(conn, String.format("alter table %s ADD %s %s %s", schemaPlusTable,
                        timeUpdatedColumn, env.getPlatform().getTimestampType(), env.getPlatform()
                                .getNullMarkerForCreateTable()));
                jdbc.execute(
                        conn, String.format("UPDATE %s SET %s = %s", schemaPlusTable, timeUpdatedColumn, "'"
                                + TIMESTAMP_FORMAT.print(new DateTime()) + "'"));
            }
            if (artifactTable.getColumn(timeInsertedColumn) == null) {
                jdbc.execute(conn, String.format("alter table %s ADD %s %s %s", schemaPlusTable,
                        timeInsertedColumn, env.getPlatform().getTimestampType(), env.getPlatform()
                                .getNullMarkerForCreateTable()));
                jdbc.execute(conn, String.format("UPDATE %s SET %s = %s", schemaPlusTable, timeInsertedColumn, timeUpdatedColumn));
            }
            if (artifactTable.getColumn(rollbackContentColumn) == null) {
                jdbc.execute(conn, String.format("alter table %s ADD %s %s %s", schemaPlusTable,
                        rollbackContentColumn, env.getPlatform().getTextType(), env.getPlatform().getNullMarkerForCreateTable()));
                // for the 3.12.0 release, we will also update the METADATA changeType value to STATICDATA
                jdbc.execute(conn, String.format("UPDATE %1$s SET %2$s='%3$s' WHERE %2$s='%4$s'",
                        schemaPlusTable, changeTypeColumn, ChangeType.STATICDATA_STR,
                        OLD_STATICDATA_CHANGETYPE));
            }
            if (artifactTable.getColumn(insertDeployExecutionIdColumn) == null) {
                jdbc.execute(conn, String.format("alter table %s ADD %s %s %s", schemaPlusTable,
                        insertDeployExecutionIdColumn, env.getPlatform().getBigIntType(), env.getPlatform().getNullMarkerForCreateTable()));

                // If this column doesn't exist, it means we've just moved to the version w/ the DeployExecution table.
                // Let's add a row here to backfill the data.
                DeployExecution deployExecution = new DeployExecutionImpl("backfill", "backfill", schema, "0.0.0", getCurrentTimestamp(), false, false, null, "backfill", Sets.immutable.<DeployExecutionAttribute>empty());
                deployExecution.setStatus(DeployExecutionStatus.SUCCEEDED);
                deployExecutionDao.persistNewSameContext(conn, deployExecution, physicalSchema);
                jdbc.execute(conn, String.format("UPDATE %s SET %s = %s", schemaPlusTable, insertDeployExecutionIdColumn, deployExecution.getId()));
            }
            if (artifactTable.getColumn(updateDeployExecutionIdColumn) == null) {
                jdbc.execute(conn, String.format("alter table %s ADD %s %s %s", schemaPlusTable,
                        updateDeployExecutionIdColumn, env.getPlatform().getBigIntType(), env.getPlatform().getNullMarkerForCreateTable()));
                jdbc.execute(conn, String.format("UPDATE %s SET %s = %s", schemaPlusTable, updateDeployExecutionIdColumn, insertDeployExecutionIdColumn));
            }
        }
    }

    /**
     * SQL for the 5.0.x upgrade.
     * We keep in separate methods to allow for easy testing of the upgrades in SameSchemaChangeAuditDaoTest for different DBMSs
     */
    @VisibleForTesting
    String get5_0Sql(PhysicalSchema physicalSchema) {
        return env.getAuditTableSql() != null ? env.getAuditTableSql() : String.format("CREATE TABLE " + env.getPlatform().getSchemaPrefix(physicalSchema) + dbChangeTable + " ( \n" +
                "    ARTFTYPE    \tVARCHAR(31) NOT NULL,\n" +
                "    " + changeNameColumn + "\tVARCHAR(255) NOT NULL,\n" +
                "    OBJECTNAME  \tVARCHAR(255) NOT NULL,\n" +
                "    ACTIVE      \tINTEGER %1$s,\n" +
                "    " + changeTypeColumn + "  \tVARCHAR(255) %1$s,\n" +
                "    CONTENTHASH \tVARCHAR(255) %1$s,\n" +
                "    DBSCHEMA    \tVARCHAR(255) %1$s,\n" +
                "    " + deployUserIdColumn + "    \tVARCHAR(32) %1$s,\n" +
                "    " + timeInsertedColumn + "   \t" + env.getPlatform().getTimestampType() + " %1$s,\n" +
                "    " + timeUpdatedColumn + "    \t" + env.getPlatform().getTimestampType() + " %1$s,\n" +
                "    " + rollbackContentColumn + "\t" + env.getPlatform().getTextType() + " %1$s,\n" +
                "    CONSTRAINT ARTDEFPK PRIMARY KEY(" + changeNameColumn + ",OBJECTNAME)\n" +
                ") %2$s\n", env.getPlatform().getNullMarkerForCreateTable(), env.getPlatform().getTableSuffixSql(env));
    }

    /**
     * SQL for the 5.1.x upgrade.
     * We keep in separate methods to allow for easy testing of the upgrades in SameSchemaChangeAuditDaoTest for different DBMSs
     */
    @VisibleForTesting
    String get5_1Sql(PhysicalSchema physicalSchema) {
        return env.getAuditTableSql() != null ? env.getAuditTableSql() : String.format("CREATE TABLE " + env.getPlatform().getSchemaPrefix(physicalSchema) + dbChangeTable + " ( \n" +
                "    ARTFTYPE    \tVARCHAR(31) NOT NULL,\n" +
                "    " + changeNameColumn + "\tVARCHAR(255) NOT NULL,\n" +
                "    OBJECTNAME  \tVARCHAR(255) NOT NULL,\n" +
                "    ACTIVE      \tINTEGER %1$s,\n" +
                "    " + changeTypeColumn + "  \tVARCHAR(255) %1$s,\n" +
                "    CONTENTHASH \tVARCHAR(255) %1$s,\n" +
                "    DBSCHEMA    \tVARCHAR(255) %1$s,\n" +
                "    " + deployUserIdColumn + "    \tVARCHAR(32) %1$s,\n" +
                "    " + timeInsertedColumn + "   \t" + env.getPlatform().getTimestampType() + " %1$s,\n" +
                "    " + timeUpdatedColumn + "    \t" + env.getPlatform().getTimestampType() + " %1$s,\n" +
                "    " + rollbackContentColumn + "\t" + env.getPlatform().getTextType() + " %1$s,\n" +
                "    " + insertDeployExecutionIdColumn + "\t" + env.getPlatform().getBigIntType() + " %1$s,\n" +
                "    " + updateDeployExecutionIdColumn + "\t" + env.getPlatform().getBigIntType() + " %1$s,\n" +
                "    CONSTRAINT ARTDEFPK PRIMARY KEY(" + changeNameColumn + ",OBJECTNAME)\n" +
                ") %2$s\n", env.getPlatform().getNullMarkerForCreateTable(), env.getPlatform().getTableSuffixSql(env));
    }

    @Override
    public void insertNewChange(final Change change, final DeployExecution deployExecution) {
        sqlExecutor.executeWithinContext(change.getPhysicalSchema(env), new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                insertNewChangeInternal(conn, change, deployExecution);
            }
        });
    }

    private void insertNewChangeInternal(Connection conn, Change change, DeployExecution deployExecution) {
        JdbcHelper jdbcTemplate = sqlExecutor.getJdbcTemplate();

        Timestamp currentTimestamp = getCurrentTimestamp();
        jdbcTemplate.update(
                conn, "INSERT INTO " + env.getPlatform().getSchemaPrefix(change.getPhysicalSchema(env))
                        + dbChangeTable +
                        " (ARTFTYPE, DBSCHEMA, ACTIVE, CHANGETYPE, CONTENTHASH, " + changeNameColumn + ", OBJECTNAME, "
                        + rollbackContentColumn + ", " + deployUserIdColumn + ", " + timeInsertedColumn + ", " + timeUpdatedColumn + ", " + insertDeployExecutionIdColumn + ", " + updateDeployExecutionIdColumn + ") " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                , change instanceof ChangeIncremental ? "I" : "R"
                , change.getSchema()
                , change.isActive() ? 1 : 0
                , change.getChangeType().getName()
                , change.getContentHash()
                , change.getChangeName()
                , change.getObjectName()
                , change.getRollbackContent()
                , deployUserId
                , currentTimestamp
                , currentTimestamp
                , deployExecution.getId()
                , deployExecution.getId()
        );
    }

    @Override
    public void updateOrInsertChange(final Change change, final DeployExecution deployExecution) {
        sqlExecutor.executeWithinContext(change.getPhysicalSchema(env), new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                int numRowsUpdated = updateInternal(conn, change, deployExecution);
                if (numRowsUpdated == 0) {
                    insertNewChangeInternal(conn, change, deployExecution);
                }
            }
        });
    }

    @Override
    public ImmutableList<Change> getDeployedChanges() {
        final Function<String, String> convertDbObjectName = env.getPlatform().convertDbObjectName();

        MutableList<Change> artfs = env.getSchemaNames().toList().flatCollect(new Function<String, MutableList<Change>>() {
            @Override
            public MutableList<Change> valueOf(final String schema) {
                final PhysicalSchema physicalSchema = env.getPhysicalSchema(schema);
                return sqlExecutor.executeWithinContext(physicalSchema, new ThrowingFunction<Connection, MutableList<Change>>() {
                    @Override
                    public MutableList<Change> safeValueOf(Connection conn) throws Exception {
                        JdbcHelper jdbcTemplate = sqlExecutor.getJdbcTemplate();
                        final DaTable artifactTable = dbMetadataManager.getTableInfo(physicalSchema,
                                dbChangeTable, new DaSchemaInfoLevel().setRetrieveTableColumns(true));

                        if (artifactTable == null) {
                            // If the artifact tables does not exist, then return empty list for that schema
                            return Lists.mutable.empty();
                        }

                        return ListAdapter.adapt(jdbcTemplate.query(
                                conn, "SELECT * FROM " + env.getPlatform().getSchemaPrefix(physicalSchema) + dbChangeTable
                                        + " WHERE DBSCHEMA = '" + schema + "'", new MapListHandler())).collect(new Function<Map<String, Object>, Change>() {
                            @Override
                            public Change valueOf(Map<String, Object> resultSet) {
                                String artfType = (String) resultSet.get(convertDbObjectName.valueOf(resolveColumnName("ARTFTYPE")));
                                Change artf;
                                if (artfType.equals("I")) {
                                    artf = new ChangeIncremental();
                                } else if (artfType.equals("R")) {
                                    artf = new ChangeRerunnable();
                                } else {
                                    throw new IllegalArgumentException("This type does not exist " + artfType);
                                }

                                artf.setChangeName((String) resultSet.get(convertDbObjectName.valueOf(resolveColumnName(changeNameColumn))));
                                // these are repeated semi-often; hence the intern
                                artf.setObjectName(InternMap.instance().intern((String) resultSet.get(
                                        convertDbObjectName.valueOf(resolveColumnName("OBJECTNAME")))));

                                artf.setActive(env.getPlatform().getIntegerValue(resultSet.get(convertDbObjectName.valueOf(resolveColumnName("ACTIVE")))) == 1);
                                // change METADATA to STATICDATA for backward compatability
                                String changeType = (String) resultSet.get(convertDbObjectName.valueOf(resolveColumnName("CHANGETYPE")));
                                changeType = changeType.equals(OLD_STATICDATA_CHANGETYPE) ? ChangeType.STATICDATA_STR : changeType;
                                artf.setChangeType(env.getPlatform().getChangeType(changeType));

                                artf.setContentHash((String) resultSet.get(convertDbObjectName.valueOf(resolveColumnName("CONTENTHASH"))));
                                // these are repeated often
                                artf.setSchema(InternMap.instance().intern((String) resultSet.get(
                                        convertDbObjectName.valueOf(resolveColumnName("DBSCHEMA")))));

                                artf.setTimeInserted(env.getPlatform().getTimestampValue(resultSet.get(convertDbObjectName.valueOf(resolveColumnName(timeInsertedColumn)))));
                                artf.setTimeUpdated(env.getPlatform().getTimestampValue(resultSet.get(convertDbObjectName.valueOf(resolveColumnName(timeUpdatedColumn)))));

                                // for backward compatibility, make sure the ROLLBACKCONTENT column exists
                                if (artifactTable.getColumn(rollbackContentColumn) != null) {
                                    artf.setRollbackContent((String) resultSet.get(rollbackContentColumn));
                                }
                                return artf;
                            }
                        });
                    }
                });
            }
        });

        MutableList<Change> incrementalChanges = artfs.reject(Predicates.attributePredicate(Change.TO_CHANGE_TYPE, ChangeType.IS_RERUNNABLE));
        MutableListMultimap<ObjectKey, Change> incrementalChangeMap = incrementalChanges.groupBy(Change.TO_OBJECT_KEY);
        for (RichIterable<Change> objectChanges : incrementalChangeMap.multiValuesView()) {
            MutableList<Change> sortedObjectChanges = objectChanges.toSortedListBy(Change.TO_TIME_INSERTED);
            sortedObjectChanges.forEachWithIndex(new ObjectIntProcedure<Change>() {
                @Override
                public void value(Change each, int index) {
                    each.setOrderWithinObject(5000 + index);
                }
            });
        }

        return artfs.toSet().toList()
                .select(Predicates.attributeIn(Change.TO_SCHEMA, env.getSchemaNames())).toImmutable();
    }

    private int updateDeployedArtifactVersionInternal(Connection conn, Change artifact, String newHash, DeployExecution deployExecution) {
        artifact.setContentHash(newHash);
        return updateInternal(conn, artifact, deployExecution);
    }

    @Override
    public void deleteChange(final Change change) {
        sqlExecutor.executeWithinContext(change.getPhysicalSchema(env), new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                sqlExecutor.getJdbcTemplate().update(
                        conn, "DELETE FROM " + env.getPlatform().getSchemaPrefix(change.getPhysicalSchema(env))
                                + dbChangeTable + " WHERE " + changeNameColumn + " = ? AND OBJECTNAME = ?"
                        , change.getChangeName(), change.getObjectName());
            }
        });
    }

    @Override
    public void deleteObjectChanges(final Change change) {
        sqlExecutor.executeWithinContext(change.getPhysicalSchema(env), new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                sqlExecutor.getJdbcTemplate().update(
                        conn, "DELETE FROM " + env.getPlatform().getSchemaPrefix(change.getPhysicalSchema(env))
                                + dbChangeTable + " WHERE OBJECTNAME = ? AND CHANGETYPE = ?",
                        change.getObjectName(),
                        change.getChangeType().getName()
                );

                // TODO delete this eventually
                sqlExecutor.getJdbcTemplate().update(
                        conn, "DELETE FROM " + env.getPlatform().getSchemaPrefix(change.getPhysicalSchema(env))
                                + dbChangeTable + " WHERE OBJECTNAME = ? AND CHANGETYPE = ?",
                        change.getObjectName(),
                        "GRANT"
                );
            }
        });
    }

    private int updateInternal(Connection conn, Change artifact, DeployExecution deployExecution) {
        return sqlExecutor.getJdbcTemplate().update(
                conn, "UPDATE " + env.getPlatform().getSchemaPrefix(artifact.getPhysicalSchema(env)) + dbChangeTable
                        + " SET " +
                        "ARTFTYPE = ?, " +
                        "DBSCHEMA = ?, " +
                        "ACTIVE = ?, " +
                        "CHANGETYPE = ?, " +
                        "CONTENTHASH = ?, " +
                        rollbackContentColumn + " = ?, " +
                        deployUserIdColumn + " = ?, " +
                        timeUpdatedColumn + " = ?, " +
                        updateDeployExecutionIdColumn + " = ? " +
                        "WHERE " + changeNameColumn + " = ? AND OBJECTNAME = ?"
                , artifact instanceof ChangeIncremental ? "I" : "R"
                , artifact.getSchema()
                , artifact.isActive() ? 1 : 0
                , artifact.getChangeType().getName()
                , artifact.getContentHash()
                , artifact.getRollbackContent()
                , deployUserId
                , getCurrentTimestamp()
                , deployExecution.getId()
                , artifact.getChangeName()
                , artifact.getObjectName()
        );
    }

    private Timestamp getCurrentTimestamp() {
        return new Timestamp(new DateTime().getMillis());
    }

    private String resolveColumnName(String colName) {
        return colName;
    }
}
