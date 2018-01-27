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
import java.util.List;
import java.util.Map;

import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.appdata.DeployExecutionAttribute;
import com.gs.obevo.api.appdata.DeployExecutionAttributeImpl;
import com.gs.obevo.api.appdata.DeployExecutionImpl;
import com.gs.obevo.api.appdata.DeployExecutionStatus;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployExecutionDao;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Grant;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.api.appdata.Permission;
import com.gs.obevo.db.api.platform.DbChangeTypeBehavior;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry;
import com.gs.obevo.util.VisibleForTesting;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang3.mutable.MutableInt;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.function.checked.ThrowingFunction;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.Interval;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SameSchemaDeployExecutionDao implements DeployExecutionDao {
    private static final Logger LOG = LoggerFactory.getLogger(SameSchemaDeployExecutionDao.class);
    // input param
    private final SqlExecutor sqlExecutor;
    private final JdbcHelper jdbc;
    private final DbMetadataManager dbMetadataManager;
    private final ImmutableSet<PhysicalSchema> physicalSchemas;
    private final String tableSqlSuffix;
    private final DbEnvironment env;
    private final ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry;

    // dialect
    private final DbPlatform platform;

    // set by dialect
    private final String deployExecutionTableName;
    private final String deployExecutionAttributeTableName;
    private final String idColName;
    private final String statusColName;
    private final String deployTimeColName;
    private final String executorIdColName;
    private final String toolVersionColName;
    private final String initCommandColName;
    private final String rollbackCommandColName;
    private final String requesterIdColName;
    private final String reasonColName;
    private final String deployExecutionIdColName;
    private final String attrNameColName;
    private final String attrValueColName;
    private final String productVersionColName;
    private final String dbSchemaColName;
    private final ImmutableList<String> allMainColumns;
    private final ImmutableList<String> allAttrColumns;
    private final ImmutableMap<PhysicalSchema, MutableInt> nextIdBySchema;

    public SameSchemaDeployExecutionDao(SqlExecutor sqlExecutor, DbMetadataManager dbMetadataManager, DbPlatform platform, ImmutableSet<PhysicalSchema> physicalSchemas, String tableSqlSuffix, DbEnvironment env, ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry) {
        this.sqlExecutor = sqlExecutor;
        this.jdbc = sqlExecutor.getJdbcTemplate();
        this.dbMetadataManager = dbMetadataManager;
        this.platform = platform;
        this.physicalSchemas = physicalSchemas;
        this.nextIdBySchema = physicalSchemas
                .toMap(Functions.<PhysicalSchema>getPassThru(), new Function<PhysicalSchema, MutableInt>() {
                    @Override
                    public MutableInt valueOf(PhysicalSchema object) {
                        return new MutableInt(1);
                    }
                })
                .toImmutable();
        this.tableSqlSuffix = tableSqlSuffix;
        this.env = env;
        this.changeTypeBehaviorRegistry = changeTypeBehaviorRegistry;

        Function<String, String> convertDbObjectName = platform.convertDbObjectName();
        this.deployExecutionTableName = convertDbObjectName.valueOf(DEPLOY_EXECUTION_TABLE_NAME);
        this.deployExecutionAttributeTableName = convertDbObjectName.valueOf(DEPLOY_EXECUTION_ATTRIBUTE_TABLE_NAME);
        this.idColName = convertDbObjectName.valueOf("ID");
        this.statusColName = convertDbObjectName.valueOf("STATUS");
        this.deployTimeColName = convertDbObjectName.valueOf("DEPLOYTIME");
        this.executorIdColName = convertDbObjectName.valueOf("EXECUTORID");
        this.toolVersionColName = convertDbObjectName.valueOf("TOOLVERSION");
        this.initCommandColName = convertDbObjectName.valueOf("INIT_COMMAND");
        this.rollbackCommandColName = convertDbObjectName.valueOf("ROLLBACK_COMMAND");
        this.requesterIdColName = convertDbObjectName.valueOf("REQUESTERID");
        this.reasonColName = convertDbObjectName.valueOf("REASON");
        this.productVersionColName = convertDbObjectName.valueOf("PRODUCTVERSION");
        this.dbSchemaColName = convertDbObjectName.valueOf("DBSCHEMA");
        this.allMainColumns = Lists.immutable.with(idColName, statusColName, deployTimeColName, executorIdColName, toolVersionColName, initCommandColName, rollbackCommandColName, requesterIdColName, reasonColName, dbSchemaColName, productVersionColName);

        this.deployExecutionIdColName = convertDbObjectName.valueOf("DEPLOYEXECUTIONID");
        this.attrNameColName = convertDbObjectName.valueOf("ATTRNAME");
        this.attrValueColName = convertDbObjectName.valueOf("ATTRVALUE");
        this.allAttrColumns = Lists.immutable.with(deployExecutionIdColName, attrNameColName, attrValueColName);
    }

    @Override
    public void init() {
        for (final PhysicalSchema physicalSchema : physicalSchemas) {
            sqlExecutor.executeWithinContext(physicalSchema, new Procedure<Connection>() {
                @Override
                public void value(Connection conn) {
                    init(conn, physicalSchema);
                }
            });
        }
    }

    private void init(Connection conn, PhysicalSchema physicalSchema) {
        DbChangeTypeBehavior tableChangeType = (DbChangeTypeBehavior) changeTypeBehaviorRegistry.getChangeTypeBehavior(ChangeType.TABLE_STR);

        if (!isDaoInitialized(physicalSchema)) {
            // create main table
            jdbc.execute(conn, get5_3TableSql(physicalSchema));

            tableChangeType.applyGrants(conn, physicalSchema, deployExecutionTableName, Lists.immutable.with(new Permission("artifactTable",
                    Lists.immutable.with(new Grant(Lists.immutable.with("SELECT"), Multimaps.immutable.list.with(GrantTargetType.PUBLIC, "PUBLIC"))))));

            // create attr table
            jdbc.execute(conn, get5_2AttrTableSql(physicalSchema));

            tableChangeType.applyGrants(conn, physicalSchema, deployExecutionAttributeTableName, Lists.immutable.with(new Permission("artifactTable",
                    Lists.immutable.with(new Grant(Lists.immutable.with("SELECT"), Multimaps.immutable.list.with(GrantTargetType.PUBLIC, "PUBLIC"))))));
        } else {
            DaTable executionTable = this.dbMetadataManager.getTableInfo(physicalSchema, deployExecutionTableName, new DaSchemaInfoLevel().setRetrieveTables(true).setRetrieveTableColumns(true));

            if (executionTable.getColumn(productVersionColName) == null) {
                // add the column if missing
                jdbc.execute(conn, String.format("ALTER TABLE %s%s ADD %s %s %s", platform.getSubschemaPrefix(physicalSchema), deployExecutionTableName,
                        productVersionColName, "VARCHAR(255)", platform.getNullMarkerForCreateTable()));
                backfillProductVersionColumn(conn, physicalSchema);
            }

            if (executionTable.getColumn(dbSchemaColName) == null) {
                // add the column if missing
                jdbc.execute(conn, String.format("ALTER TABLE %s%s ADD %s %s %s", platform.getSubschemaPrefix(physicalSchema), deployExecutionTableName,
                        dbSchemaColName, "VARCHAR(255)", platform.getNullMarkerForCreateTable()));

                // let's now try to backfill the column, but it's tricky as we may be missing some information
                backfillDbSchemaColumn(conn, physicalSchema);
            }
        }

        Long maxId = getMaxId(conn, physicalSchema);
        nextIdBySchema.get(physicalSchema).setValue(maxId != null ? maxId.longValue() + 1 : 1);
    }

    /**
     * SQL for the 5.2.x upgrade.
     * We keep in separate methods to allow for easy testing of the upgrades in SameSchemaChangeAuditDaoTest for different DBMSs
     */
    @VisibleForTesting
    String get5_2AttrTableSql(PhysicalSchema physicalSchema) {
        return "CREATE TABLE " + platform.getSchemaPrefix(physicalSchema) + deployExecutionAttributeTableName + " (" +
                deployExecutionIdColName + " " + platform.getBigIntType() + " NOT NULL," +
                attrNameColName + " VARCHAR(128) NOT NULL," +
                attrValueColName + " VARCHAR(128) NOT NULL" +
                ")" + tableSqlSuffix;
    }

    /**
     * SQL for the 5.2.x upgrade.
     * We keep in separate methods to allow for easy testing of the upgrades in SameSchemaChangeAuditDaoTest for different DBMSs
     */
    @VisibleForTesting
    String get5_2TableSql(PhysicalSchema physicalSchema) {
        return "CREATE TABLE " + platform.getSchemaPrefix(physicalSchema) + deployExecutionTableName + " (" +
                idColName + " " + platform.getBigIntType() + " NOT NULL," +
                statusColName + " CHAR(1) NOT NULL," +
                deployTimeColName + " " + platform.getTimestampType() + " NOT NULL," +
                executorIdColName + " VARCHAR(128) NOT NULL," +
                toolVersionColName + " VARCHAR(32) NOT NULL," +
                initCommandColName + " INTEGER NOT NULL," +
                rollbackCommandColName + " INTEGER NOT NULL," +
                requesterIdColName + " VARCHAR(128) " + platform.getNullMarkerForCreateTable() + "," +
                reasonColName + " VARCHAR(128) " + platform.getNullMarkerForCreateTable() + "," +
                "CONSTRAINT DEPL_EXEC_PK PRIMARY KEY (" + idColName + ")" +
                ")" + tableSqlSuffix;
    }

    /**
     * SQL for the 5.3.x upgrade.
     * We keep in separate methods to allow for easy testing of the upgrades in SameSchemaChangeAuditDaoTest for different DBMSs
     */
    @VisibleForTesting
    private String get5_3TableSql(PhysicalSchema physicalSchema) {
        return "CREATE TABLE " + platform.getSchemaPrefix(physicalSchema) + deployExecutionTableName + " (" +
                idColName + " " + platform.getBigIntType() + " NOT NULL," +
                statusColName + " CHAR(1) NOT NULL," +
                deployTimeColName + " " + platform.getTimestampType() + " NOT NULL," +
                executorIdColName + " VARCHAR(128) NOT NULL," +
                toolVersionColName + " VARCHAR(32) NOT NULL," +
                initCommandColName + " INTEGER NOT NULL," +
                rollbackCommandColName + " INTEGER NOT NULL," +
                requesterIdColName + " VARCHAR(128) " + platform.getNullMarkerForCreateTable() + "," +
                reasonColName + " VARCHAR(128) " + platform.getNullMarkerForCreateTable() + "," +
                productVersionColName + " VARCHAR(255) " + platform.getNullMarkerForCreateTable() + "," +
                dbSchemaColName + " VARCHAR(255) " + platform.getNullMarkerForCreateTable() + "," +
                "CONSTRAINT DEPL_EXEC_PK PRIMARY KEY (" + idColName + ")" +
                ")" + tableSqlSuffix;
    }

    /**
     * Check that the DAO tables have been created. This is needed:  1) for the initialization of this DAO for writing
     * data  2) to allow the read calls to proceed even if tables aren't created - in that case, they would return no
     * data (and should not try to execute a SQL against a non-existent table and fail).
     */
    private boolean isDaoInitialized(PhysicalSchema physicalSchema) {
        boolean mainTableExists = getTable(physicalSchema, deployExecutionTableName) != null;
        boolean attrTableExists = getTable(physicalSchema, deployExecutionAttributeTableName) != null;

        if (mainTableExists && attrTableExists) {
            return true;
        } else if (!mainTableExists && !attrTableExists) {
            return false;
        } else {
            throw new IllegalStateException("Inconsistent table state - main table " + (mainTableExists ? "exists" : "doesn't exist") + " but attr table " + (attrTableExists ? "exists" : "doesn't exist")
                    + ", indicating a partial initialization; please resolve this situation (both tables should either be present or absent for the initialization check");
        }
    }

    private Long getMaxId(Connection conn, PhysicalSchema physicalSchema) {
        if (!isDaoInitialized(physicalSchema)) {
            return null;
        }
        return jdbc.queryForLong(conn, "SELECT max(" + idColName + ") FROM " + platform.getSchemaPrefix(physicalSchema) + deployExecutionTableName);
    }

    private DaTable getTable(PhysicalSchema physicalSchema, String tableName) {
        return this.dbMetadataManager.getTableInfo(physicalSchema, tableName, new DaSchemaInfoLevel().setRetrieveTables(true));
    }

    @Override
    public void persistNew(final DeployExecution entry, final PhysicalSchema physicalSchema) {
        sqlExecutor.executeWithinContext(physicalSchema, new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                persistNewSameContext(conn, entry, physicalSchema);
            }
        });
    }

    void persistNewSameContext(Connection conn, final DeployExecution entry, final PhysicalSchema physicalSchema) {
        final String insertColumnString = allMainColumns.makeString("(", ", ", ")");
        final String insertValueString = Interval.oneTo(allMainColumns.size()).collect(Functions.getFixedValue("?")).makeString("(", ", ", ")");

        final String attrInsertColumnString = allAttrColumns.makeString("(", ", ", ")");
        final String attrInsertValueString = Interval.oneTo(allAttrColumns.size()).collect(Functions.getFixedValue("?")).makeString("(", ", ", ")");

        ((DeployExecutionImpl) entry).setId(nextIdBySchema.get(physicalSchema).longValue());
        jdbc.update(conn, "INSERT INTO " + platform.getSchemaPrefix(physicalSchema) + deployExecutionTableName + " " +
                        insertColumnString + " " +
                        "VALUES " + insertValueString,
                entry.getId(),
                String.valueOf(entry.getStatus().getStatusCode()),  // must convert char to string for Sybase compatibility
                entry.getDeployTime(),
                entry.getExecutorId(),
                entry.getToolVersion() != null ? entry.getToolVersion() : "0.0.0",
                entry.isInit() ? 1 : 0,
                entry.isRollback() ? 1 : 0,
                entry.getRequesterId(),
                entry.getReason(),
                entry.getSchema(),
                entry.getProductVersion()
        );

        for (DeployExecutionAttribute deployExecutionAttribute : entry.getAttributes()) {
            jdbc.update(conn, "INSERT INTO " + platform.getSchemaPrefix(physicalSchema) + deployExecutionAttributeTableName + " " +
                            attrInsertColumnString + " " +
                            "VALUES " + attrInsertValueString,
                    entry.getId(),
                    deployExecutionAttribute.getName(),
                    deployExecutionAttribute.getValue()
            );
        }

        nextIdBySchema.get(physicalSchema).increment();
    }

    @Override
    public void update(final DeployExecution entry) {
        for (final PhysicalSchema physicalSchema : physicalSchemas) {
            sqlExecutor.executeWithinContext(physicalSchema, new Procedure<Connection>() {
                @Override
                public void value(Connection conn) {
                    jdbc.update(conn, "UPDATE " + platform.getSchemaPrefix(physicalSchema) + deployExecutionTableName + " " +
                                    "SET " + statusColName + " = ? " +
                                    "WHERE " + idColName + " = ? ",
                            String.valueOf(entry.getStatus().getStatusCode()),
                            entry.getId()
                    );
                }
            });
        }
    }

    @Override
    public ImmutableCollection<DeployExecution> getDeployExecutions(final String schema) {
        return sqlExecutor.executeWithinContext(env.getPhysicalSchema(schema), new ThrowingFunction<Connection, ImmutableCollection<DeployExecution>>() {
            @Override
            public ImmutableCollection<DeployExecution> safeValueOf(Connection conn) throws Exception {
                return getDeployExecutions(conn, schema, null);
            }
        });
    }

    @Override
    public DeployExecution getLatestDeployExecution(final String schema) {
        final PhysicalSchema physicalSchema = env.getPhysicalSchema(schema);
        return sqlExecutor.executeWithinContext(physicalSchema, new ThrowingFunction<Connection, DeployExecution>() {
            @Override
            public DeployExecution safeValueOf(Connection conn) throws Exception {
                Long maxId = getMaxId(conn, physicalSchema);
                if (maxId == null) {
                    return null;
                }
                ImmutableCollection<DeployExecution> deployExecutions = getDeployExecutions(conn, schema, maxId);

                if (deployExecutions.size() == 1) {
                    return deployExecutions.iterator().next();
                } else if (deployExecutions.size() > 1) {
                    throw new IllegalStateException("Something is wrong w/ DB state; cannot have multiple deployExecutions for ID " + maxId + ": " + deployExecutions);
                } else {
                    // deployExecutions == 0
                    throw new RuntimeException("Something is wrong w/ logic; found maxId " + maxId + " but could not query DB entries for that ID");
                }
            }
        });
    }

    private ImmutableCollection<DeployExecution> getDeployExecutions(Connection conn, final String schema, Long idToQuery) {
        PhysicalSchema physicalSchema = env.getPhysicalSchema(schema);
        if (!isDaoInitialized(physicalSchema)) {
            return Lists.immutable.empty();
        }

        DaTable tableInfo = this.dbMetadataManager.getTableInfo(physicalSchema, deployExecutionTableName, new DaSchemaInfoLevel().setRetrieveTables(true).setRetrieveTableColumns(true));

        MutableList<String> mainWhereClauses = Lists.mutable.empty();
        MutableList<String> attrWhereClauses = Lists.mutable.empty();

        // account for the 5.2.x -> 5.3.0 version and rollback integration upgrade by checking for the db schema column
        if (tableInfo.getColumn(dbSchemaColName) != null) {
            mainWhereClauses.add(dbSchemaColName + " = '" + schema + "'");
        }
        if (idToQuery != null) {
            mainWhereClauses.add(idColName + " = " + idToQuery.longValue());
            attrWhereClauses.add(deployExecutionIdColName + " = " + idToQuery.longValue());
        }

        String mainWhereClause = mainWhereClauses.notEmpty() ? " WHERE " + mainWhereClauses.makeString(" AND ") : "";
        String attrWhereClause = attrWhereClauses.notEmpty() ? " WHERE " + attrWhereClauses.makeString(" AND ") : "";

        String mainQuery = "SELECT * FROM " + platform.getSchemaPrefix(physicalSchema) + deployExecutionTableName + mainWhereClause;
        String attrQuery = "SELECT * FROM " + platform.getSchemaPrefix(physicalSchema) + deployExecutionAttributeTableName + " " + attrWhereClause;

        final MutableListMultimap<Long, DeployExecutionAttribute> attrsById = Multimaps.mutable.list.empty();

        for (Map<String, Object> attrResult : ListAdapter.adapt(jdbc.query(conn, attrQuery, new MapListHandler()))) {
            long id = platform.getLongValue(attrResult.get(deployExecutionIdColName)).longValue();

            DeployExecutionAttribute attr = new DeployExecutionAttributeImpl(
                    (String) attrResult.get(attrNameColName),
                    (String) attrResult.get(attrValueColName)
            );

            attrsById.put(id, attr);
        }

        return ListAdapter.adapt(jdbc.query(conn, mainQuery, new MapListHandler())).collect(new Function<Map<String, Object>, DeployExecution>() {
            @Override
            public DeployExecution valueOf(Map<String, Object> result) {
                long id = platform.getLongValue(result.get(idColName)).longValue();
                DeployExecutionStatus status = DeployExecutionStatus.IN_PROGRESS.valueOfStatusCode(((String) result.get(statusColName)).charAt(0));
                Timestamp deployTime = platform.getTimestampValue(result.get(deployTimeColName));
                String executorId = (String) result.get(executorIdColName);
                String toolVersion = (String) result.get(toolVersionColName);
                boolean init = platform.getIntegerValue(result.get(initCommandColName)).intValue() == 1;
                boolean rollback = platform.getIntegerValue(result.get(rollbackCommandColName)).intValue() == 1;
                String requesterId = (String) result.get(requesterIdColName);
                String reason = (String) result.get(reasonColName);
                String productVersion = (String) result.get(productVersionColName);

                ImmutableSet<DeployExecutionAttribute> deployExecutionAttributes = attrsById.get(id).toSet().toImmutable();

                DeployExecutionImpl deployExecution = new DeployExecutionImpl(requesterId, executorId, schema, toolVersion, deployTime, init, rollback, productVersion, reason, deployExecutionAttributes);
                deployExecution.setId(id);
                deployExecution.setStatus(status);

                return deployExecution;
            }
        }).toImmutable();
    }

    @Override
    public String getExecutionContainerName() {
        return deployExecutionTableName;
    }

    @Override
    public String getExecutionAttributeContainerName() {
        return deployExecutionAttributeTableName;
    }

    private void backfillProductVersionColumn(Connection conn, PhysicalSchema physicalSchema) {
        // backfill the product version column from conduit, if available
        List<Map<String, Object>> idVersions = jdbc.queryForList(conn, String.format("SELECT %1$s, %2$s FROM %3$s WHERE %4$s = 'conduit.version.name'",
                deployExecutionIdColName, attrValueColName, platform.getSubschemaPrefix(physicalSchema) + deployExecutionAttributeTableName, attrNameColName
        ));
        for (Map<String, Object> idVersion : idVersions) {
            long id = platform.getLongValue(idVersion.get(deployExecutionIdColName)).longValue();
            String version = (String) idVersion.get(attrValueColName);
            jdbc.execute(conn, "UPDATE " + platform.getSchemaPrefix(physicalSchema) + deployExecutionTableName + " " +
                    "SET " + productVersionColName + " = '" + version + "' " +
                    "WHERE " + idColName + " = " + id);
        }
    }

    private void backfillDbSchemaColumn(Connection conn, PhysicalSchema physicalSchema) {
        // first, get a mapping of all the IDs to DB schemas from the artifactdeployment table. Note that technically
        // we may have lost information as rows can get updated in place, but we'll make do here
        final MutableSetMultimap<Long, String> execIdToSchemaMap = Multimaps.mutable.set.empty();
        for (Map<String, Object> tempExecIdToSchemaMap : jdbc.queryForList(conn, "SELECT DISTINCT INSERTDEPLOYID, UPDATEDEPLOYID, DBSCHEMA FROM " + platform.getSubschemaPrefix(physicalSchema) + "ARTIFACTDEPLOYMENT")) {
            Long insertDeployId = platform.getLongValue(tempExecIdToSchemaMap.get("INSERTDEPLOYID"));
            Long updateDeployId = platform.getLongValue(tempExecIdToSchemaMap.get("UPDATEDEPLOYID"));
            String dbSchema = (String) tempExecIdToSchemaMap.get("DBSCHEMA");
            if (insertDeployId != null) {
                execIdToSchemaMap.put(insertDeployId, dbSchema);
            }
            if (updateDeployId != null) {
                execIdToSchemaMap.put(updateDeployId, dbSchema);
            }
        }

        // find the list of distinct schemas from that list
        MutableSet<String> dbSchemas = execIdToSchemaMap.valuesView().toSet();

        if (dbSchemas.size() == 1) {
            // If we only found 1 schema in ARTIFACTDEPLOYMENT, then we can assume that all the IDs in ARTIFACTEXECUTION
            // also belonged to that schema. So we go w/ the simple update
            jdbc.execute(conn, "UPDATE " + platform.getSubschemaPrefix(physicalSchema) + deployExecutionTableName + " " +
                    "SET " + dbSchemaColName + " = '" + dbSchemas.getFirst() + "' ");
        } else if (dbSchemas.size() > 1) {
            // If not, then we need to look a bit deeper to try to match the ID to a schema

            // First compare based on the version name (hoping that all the deployments of a version are traced back to only
            MutableListMultimap<String, Long> versionToIdsMap = Multimaps.mutable.list.empty();
            for (Map<String, Object> idToVersionMap : jdbc.queryForList(conn, "SELECT " + idColName + ", " + productVersionColName + " FROM " + platform.getSubschemaPrefix(physicalSchema) + deployExecutionTableName)) {
                versionToIdsMap.put((String) idToVersionMap.get(productVersionColName), platform.getLongValue(idToVersionMap.get(idColName)).longValue());
            }

            for (Pair<String, RichIterable<Long>> versionIdsPair : versionToIdsMap.keyMultiValuePairsView()) {
                RichIterable<Long> ids = versionIdsPair.getTwo();

                // Find all the schemas matched to the version
                MutableSet<String> versionSchemas = ids.flatCollect(new Function<Long, Iterable<String>>() {
                    @Override
                    public Iterable<String> valueOf(Long aLong) {
                        return execIdToSchemaMap.get(aLong);
                    }
                }, Sets.mutable.<String>empty());

                for (Long id : ids) {
                    // iterate for each ID of the version

                    if (versionSchemas.size() == 1) {
                        // If we just had 1 schema for all the versions, then do the simple update
                        jdbc.execute(conn, "UPDATE " + platform.getSubschemaPrefix(physicalSchema) + deployExecutionTableName + " " +
                                "SET " + dbSchemaColName + " = '" + versionSchemas.getFirst() + "' " +
                                "WHERE " + idColName + " = " + id
                        );
                    } else {
                        // Otherwise, fall back to the schema list per id
                        String schemaToSet;
                        MutableSet<String> idSchemas = execIdToSchemaMap.get(id);
                        if (idSchemas.size() == 1) {
                            schemaToSet = idSchemas.getFirst();
                        } else if (idSchemas.size() >= 1) {
                            LOG.warn("Not expecting multiple schemas on ID {} to be defined: {} ", id, idSchemas);
                            schemaToSet = "MULTISCHEMA";
                        } else {
                            LOG.warn("No schemas found for ID {}", id, idSchemas);
                            schemaToSet = "NOSCHEMA";
                        }

                        jdbc.execute(conn, "UPDATE " + platform.getSubschemaPrefix(physicalSchema) + deployExecutionTableName + " " +
                                "SET " + dbSchemaColName + " = '" + schemaToSet + "' " +
                                "WHERE " + idColName + " = " + id
                        );
                    }
                }
            }
        }
    }
}
