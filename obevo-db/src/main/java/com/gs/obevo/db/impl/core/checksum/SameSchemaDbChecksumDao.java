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
package com.gs.obevo.db.impl.core.checksum;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Map;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeBehaviorRegistry;
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
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.function.checked.ThrowingFunction;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.joda.time.DateTime;

/**
 * {@link DbChecksumDao} implementation that writes the checksums into the same schemas as the checksum entries
 * themselves.
 */
public class SameSchemaDbChecksumDao implements DbChecksumDao {
    // input param
    private final SqlExecutor sqlExecutor;
    private final JdbcHelper jdbc;
    private final DbMetadataManager dbMetadataManager;
    private final ImmutableSet<PhysicalSchema> physicalSchemas;
    private final String tableSqlSuffix;
    private final ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry;

    // dialect
    private final DbPlatform platform;

    // set by dialect
    private final String checksumTableName;
    private final String objectTypeColumnName;
    private final String objectName1ColumnName;
    private final String objectName2ColumnName;
    private final String checksumColumnName;
    private final String timeUpdatedColumnName;

    public SameSchemaDbChecksumDao(SqlExecutor sqlExecutor, DbMetadataManager dbMetadataManager, DbPlatform platform, ImmutableSet<PhysicalSchema> physicalSchemas, String tableSqlSuffix, ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry) {
        this.sqlExecutor = sqlExecutor;
        this.jdbc = sqlExecutor.getJdbcTemplate();
        this.dbMetadataManager = dbMetadataManager;
        this.platform = platform;
        this.physicalSchemas = physicalSchemas;
        this.tableSqlSuffix = tableSqlSuffix;
        this.changeTypeBehaviorRegistry = changeTypeBehaviorRegistry;

        Function<String, String> convertDbObjectName = platform.convertDbObjectName();
        this.checksumTableName = convertDbObjectName.valueOf(SCHEMA_CHECKSUM_TABLE_NAME);
        this.objectTypeColumnName = convertDbObjectName.valueOf("OBJECTTYPE");
        this.objectName1ColumnName = convertDbObjectName.valueOf("OBJECTNAME1");
        this.objectName2ColumnName = convertDbObjectName.valueOf("OBJECTNAME2");
        this.checksumColumnName = convertDbObjectName.valueOf("CHECKSUM");
        this.timeUpdatedColumnName = convertDbObjectName.valueOf("TIME_UPDATED");
    }

    @Override
    public boolean isInitialized() {
        for (PhysicalSchema physicalSchema : physicalSchemas) {
            DaTable checksumTable = getChecksumTable(physicalSchema);
            if (checksumTable == null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void initialize() {
        for (final PhysicalSchema physicalSchema : physicalSchemas) {
            DaTable checksumTable = getChecksumTable(physicalSchema);
            if (checksumTable == null) {
                final String checksumTableSql = "CREATE TABLE " + platform.getSchemaPrefix(physicalSchema) + checksumTableName + " (" +
                        objectTypeColumnName + " VARCHAR(30) NOT NULL," +
                        objectName1ColumnName + " VARCHAR(128) NOT NULL," +
                        objectName2ColumnName + " VARCHAR(128) NOT NULL," +
                        checksumColumnName + " VARCHAR(32) NOT NULL," +
                        timeUpdatedColumnName + " " + platform.getTimestampType() + " NOT NULL," +
                        "CONSTRAINT SCH_CHKSUM_PK PRIMARY KEY (" + objectTypeColumnName + ", " + objectName1ColumnName + ", " + objectName2ColumnName + ")" +
                        ")" + tableSqlSuffix;

                sqlExecutor.executeWithinContext(physicalSchema, new Procedure<Connection>() {
                    @Override
                    public void value(Connection conn) {
                        jdbc.execute(conn, checksumTableSql);

                        DbChangeTypeBehavior tableChangeType = (DbChangeTypeBehavior)changeTypeBehaviorRegistry.getChangeTypeBehavior(ChangeType.TABLE_STR);
                        tableChangeType.applyGrants(conn, physicalSchema, checksumTableName, Lists.immutable.with(new Permission("artifacTable",
                                Lists.immutable.with(new Grant(Lists.immutable.with("SELECT"), Multimaps.immutable.list.with(GrantTargetType.PUBLIC, "PUBLIC"))))));
                    }
                });
            }
        }
    }

    @Override
    public String getChecksumContainerName() {
        return checksumTableName;
    }

    private DaTable getChecksumTable(PhysicalSchema physicalSchema) {
        return this.dbMetadataManager.getTableInfo(physicalSchema.getPhysicalName(), checksumTableName, new DaSchemaInfoLevel().setRetrieveTables(true));
    }

    @Override
    public ImmutableCollection<ChecksumEntry> getPersistedEntries(final PhysicalSchema physicalSchema) {
        return sqlExecutor.executeWithinContext(physicalSchema, new ThrowingFunction<Connection, ImmutableCollection<ChecksumEntry>>() {
            @Override
            public ImmutableCollection<ChecksumEntry> safeValueOf(Connection conn) throws Exception {
                return ListAdapter.adapt(jdbc.query(conn, "SELECT * FROM " + platform.getSchemaPrefix(physicalSchema) + checksumTableName, new MapListHandler())).collect(new Function<Map<String, Object>, ChecksumEntry>() {
                    @Override
                    public ChecksumEntry valueOf(Map<String, Object> result) {
                        return ChecksumEntry.createFromPersistence(
                                physicalSchema,
                                (String) result.get(objectTypeColumnName),
                                (String) result.get(objectName1ColumnName),
                                blankToNull((String) result.get(objectName2ColumnName)),
                                (String) result.get(checksumColumnName)
                        );
                    }
                }).toImmutable();
            }
        });
    }

    private static String blankToNull(String str) {
        return StringUtils.isBlank(str) ? null : str;
    }

    @Override
    public void persistEntry(final ChecksumEntry entry) {
        sqlExecutor.executeWithinContext(entry.getPhysicalSchema(), new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                int numRowsUpdated = updateEntry(conn, entry);
                if (numRowsUpdated == 0) {
                    insertEntry(conn, entry);
                }
            }
        });
    }

    private void insertEntry(Connection conn, ChecksumEntry entry) {
        jdbc.update(conn, "INSERT INTO " + platform.getSchemaPrefix(entry.getPhysicalSchema()) + checksumTableName + " " +
                        "(OBJECTTYPE, OBJECTNAME1, OBJECTNAME2, CHECKSUM, TIME_UPDATED) " +
                        "VALUES (?, ?, ?, ?, ?)",
                entry.getObjectType(),
                entry.getName1(),
                entry.getName2() != null ? entry.getName2() : "",
                entry.getChecksum(),
                new Timestamp(new DateTime().getMillis())
        );
    }

    private int updateEntry(Connection conn, ChecksumEntry entry) {
        return jdbc.update(conn, "UPDATE " + platform.getSchemaPrefix(entry.getPhysicalSchema()) + checksumTableName + " " +
                        "SET CHECKSUM = ?, " +
                        "TIME_UPDATED = ? " +
                        "WHERE OBJECTTYPE = ? " +
                        "AND OBJECTNAME1 = ? " +
                        "AND OBJECTNAME2 = ?",
                entry.getChecksum(),
                new Timestamp(new DateTime().getMillis()),
                entry.getObjectType(),
                entry.getName1(),
                entry.getName2() != null ? entry.getName2() : ""
        );
    }

    @Override
    public void deleteEntry(final ChecksumEntry entry) {
        sqlExecutor.executeWithinContext(entry.getPhysicalSchema(), new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                jdbc.update(conn, "DELETE FROM " + platform.getSchemaPrefix(entry.getPhysicalSchema()) + checksumTableName + " " +
                                "WHERE OBJECTTYPE = ? " +
                                "AND OBJECTNAME1 = ? " +
                                "AND OBJECTNAME2 = ?",
                        entry.getObjectType(),
                        entry.getName1(),
                        entry.getName2() != null ? entry.getName2() : ""
                );
            }
        });
    }
}
