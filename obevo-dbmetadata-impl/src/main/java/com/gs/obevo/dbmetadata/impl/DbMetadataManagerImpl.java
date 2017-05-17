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
package com.gs.obevo.dbmetadata.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.sql.DataSource;

import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaRule;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DaSequence;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DaUserType;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.dbmetadata.api.RuleBinding;
import com.gs.obevo.util.VisibleForTesting;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.crawl.SchemaCrawler;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Schema;
import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptions;
import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptionsBuilder;
import schemacrawler.schemacrawler.ExcludeAll;
import schemacrawler.schemacrawler.RegularExpressionInclusionRule;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaInfoLevel;

public class DbMetadataManagerImpl implements DbMetadataManager {
    private static final Logger LOG = LoggerFactory.getLogger(DbMetadataManagerImpl.class);

    private final DbMetadataDialect dbMetadataDialect;
    private DataSource ds;

    public DbMetadataManagerImpl(DbMetadataDialect dbMetadataDialect) {
        this.dbMetadataDialect = dbMetadataDialect;
    }

    /**
     * Currently exposed for some unit tests; eventually plan to refactor this away.
     */
    @VisibleForTesting
    public DbMetadataManagerImpl(DbMetadataDialect dbMetadataDialect, DataSource ds) {
        this.dbMetadataDialect = dbMetadataDialect;
        this.ds = ds;
    }

    @Override
    public void setDataSource(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public DaCatalog getDatabase(String physicalSchema, DaSchemaInfoLevel schemaInfoLevel, boolean searchAllTables,
            boolean searchAllProcedures) {
        return this.getDatabase(schemaInfoLevel, physicalSchema, null, null, searchAllTables, searchAllProcedures);
    }

    /**
     * We have some special logic in here to interact w/ Schema crawler, as the different RDBMS types have
     * different parameters to the JDBC metadata
     *
     * @param searchAllProcedures @return
     */
    private DaCatalog getDatabase(DaSchemaInfoLevel schemaInfoLevel, String schemaName, String tableName,
            String procedureName, boolean searchAllTables, boolean searchAllProcedures) {
        // Many of the DB metadata drivers like IQ/ASE/DB2 don't support the function metadata lookups and
        // schemacrawler complains (though the library still does the job). We set the log level here to avoid
        // excessive log messages
        java.util.logging.Logger.getLogger("schemacrawler").setLevel(Level.WARNING);
        java.util.logging.Logger.getLogger("schemacrawler.crawl.RoutineRetriever").setLevel(Level.SEVERE);
        java.util.logging.Logger.getLogger("schemacrawler.crawl.SchemaCrawler").setLevel(Level.SEVERE);

        Validate.notNull(schemaName, "Schema must be specified");
        Connection conn = null;
        try {
            conn = this.ds.getConnection();
            if (schemaName != null) {
                this.dbMetadataDialect.setSchemaOnConnection(conn, schemaName);
            }

            SchemaCrawlerOptions options = new SchemaCrawlerOptions();
            // Set what details are required in the schema - this affects the time taken to crawl the schema
            // Standard works for our use cases (e.g. columns, indices, pks)
            options.setSchemaInfoLevel(toInfoLevel(schemaInfoLevel));

            DatabaseSpecificOverrideOptionsBuilder dbSpecificOptionsBuilder = dbMetadataDialect.getDbSpecificOptionsBuilder(conn);
            DatabaseSpecificOverrideOptions dbSpecificOptions = dbSpecificOptionsBuilder.toOptions();
            this.enrichSchemaCrawlerOptions(conn, options, dbSpecificOptions, schemaName, tableName, procedureName);

            if (tableName == null && procedureName != null && !searchAllTables) {
                options.setTableInclusionRule(new ExcludeAll());
            }
            if (procedureName == null && tableName != null && !searchAllProcedures) {
                options.setRoutineInclusionRule(new ExcludeAll());
            }

            LOG.debug("Starting query for DB metadata for {}/{}/{}/{}", tableName, procedureName,
                    searchAllTables ? "searching all tables" : "", searchAllProcedures ? "searching all procedures" : "");
            final SchemaCrawler schemaCrawler = new SchemaCrawler(conn, dbSpecificOptions);

            final Catalog database;
            try {
                database = schemaCrawler.crawl(options);
            } catch (SchemaCrawlerException e) {
                if ("No matching schemas found".equals(e.getMessage())) {
                    return null;  // This is to match SchemaCrawler 9.6 behavior; need to check w/ them on this
                } else {
                    throw e;
                }
            }

            LOG.debug("Ending query for DB metadata for {}/{}/{}/{}", tableName, procedureName,
                    searchAllTables ? "searching all tables" : "", searchAllProcedures ? "searching all procedures" : "");

            this.dbMetadataDialect.validateDatabase(database, schemaName);

            SchemaStrategy schemaStrategy = dbMetadataDialect.getSchemaStrategy();
            Schema schemaReference = database.getSchemas().isEmpty() ? null : database.getSchemas().iterator().next();
            DaSchema schema = new DaSchemaImpl(schemaReference, schemaStrategy);

            ImmutableCollection<DaRoutine> extraRoutines = Lists.immutable.empty();

            if (schemaInfoLevel.isRetrieveRoutines() && (searchAllProcedures || procedureName != null)) {
                extraRoutines = this.dbMetadataDialect.searchExtraRoutines(schema, procedureName, conn);
            }

            ImmutableCollection<ExtraIndexInfo> extraConstraintIndices = schemaInfoLevel.isRetrieveTableCheckConstraints()
                    ? dbMetadataDialect.searchExtraConstraintIndices(schema, tableName, conn)
                    : Lists.immutable.<ExtraIndexInfo>empty();
            Multimap<String, ExtraIndexInfo> constraintIndices = extraConstraintIndices.groupBy(new Function<ExtraIndexInfo, String>() {
                @Override
                public String valueOf(ExtraIndexInfo object) {
                    return object.getTableName();
                }
            });

            ImmutableCollection<ExtraRerunnableInfo> extraViewInfo = schemaInfoLevel.isRetrieveViewDetails()
                    ? dbMetadataDialect.searchExtraViewInfo(schema, tableName, conn)
                    : Lists.immutable.<ExtraRerunnableInfo>empty();

            DaRoutineType routineOverrideValue = dbMetadataDialect.getRoutineOverrideValue();

            ImmutableCollection<RuleBinding> ruleBindings = schemaInfoLevel.isRetrieveRuleBindings()
                    ? dbMetadataDialect.getRuleBindings(schema, conn)
                    : Lists.immutable.<RuleBinding>empty();
            ImmutableCollection<DaRule> rules = schemaInfoLevel.isRetrieveRules()
                    ? dbMetadataDialect.searchRules(schema, conn)
                    : Lists.immutable.<DaRule>empty();
            ImmutableCollection<DaSequence> sequences = schemaInfoLevel.isRetrieveSequences()
                    ? dbMetadataDialect.searchSequences(schema, conn)
                    : Lists.immutable.<DaSequence>empty();
            ImmutableCollection<DaUserType> userTypes = schemaInfoLevel.isRetrieveUserDefinedColumnDataTypes()
                    ? dbMetadataDialect.searchUserTypes(schema, conn)
                    : Lists.immutable.<DaUserType>empty();

            return new DaCatalogImpl(database, schemaStrategy, sequences, userTypes, rules, ruleBindings, extraRoutines, constraintIndices, extraViewInfo, routineOverrideValue);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (SchemaCrawlerException e) {
            throw new RuntimeException(e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    private SchemaInfoLevel toInfoLevel(DaSchemaInfoLevel schemaInfoLevel) {
        SchemaInfoLevel otherInfoLevel = new SchemaInfoLevel();

        otherInfoLevel.setRetrieveDatabaseInfo(true);
        //otherInfoLevel.setRetrieveJdbcDriverInfo(false);  // would prefer to add this back due to issues w/ Sybase ASE; requires followup w/ SchemaCrawler team
        otherInfoLevel.setRetrieveAdditionalJdbcDriverInfo(false);  // unneeded for our use cases and causes some problems w/ some JDBC drivers

        // tables
        otherInfoLevel.setRetrieveTables(schemaInfoLevel.isRetrieveTables());
        otherInfoLevel.setRetrieveAdditionalTableAttributes(schemaInfoLevel.isRetrieveTables());

        // table columns
        otherInfoLevel.setRetrieveColumnDataTypes(schemaInfoLevel.isRetrieveTableColumns());
        otherInfoLevel.setRetrieveTableColumns(schemaInfoLevel.isRetrieveTableColumns());
        otherInfoLevel.setRetrieveAdditionalColumnAttributes(schemaInfoLevel.isRetrieveTableColumns());

        // table extras
        otherInfoLevel.setRetrieveTableConstraintDefinitions(schemaInfoLevel.isRetrieveTableCheckConstraints());
        otherInfoLevel.setRetrieveTableConstraintInformation(schemaInfoLevel.isRetrieveTableCheckConstraints());
        otherInfoLevel.setRetrieveIndexes(schemaInfoLevel.isRetrieveTableIndexes());
        otherInfoLevel.setRetrieveForeignKeys(schemaInfoLevel.isRetrieveTableForeignKeys());

        // views
        otherInfoLevel.setRetrieveViewInformation(schemaInfoLevel.isRetrieveViewDetails());

        // routines
        otherInfoLevel.setRetrieveRoutines(schemaInfoLevel.isRetrieveRoutines());
        otherInfoLevel.setRetrieveRoutineInformation(schemaInfoLevel.isRetrieveRoutineDetails());
        otherInfoLevel.setRetrieveRoutineColumns(schemaInfoLevel.isRetrieveRoutineDetails());

        // user types
        otherInfoLevel.setRetrieveUserDefinedColumnDataTypes(schemaInfoLevel.isRetrieveUserDefinedColumnDataTypes());  // TODO see if this takes care of domains

        // table trigger
        otherInfoLevel.setRetrieveTriggerInformation(false);  // will implement this later

        // synonyms
        otherInfoLevel.setRetrieveSynonymInformation(false);  // will implement this later

        // not yet pulling in privileges; may choose to do this bulk (i.e. flag for privileges pulls in privileges for all object types
        otherInfoLevel.setRetrieveTablePrivileges(false);
        otherInfoLevel.setRetrieveTableColumnPrivileges(false);

        return otherInfoLevel;
    }

    @Override
    public DaCatalog getDatabase(String physicalSchema) {
        try {
            return this.getDatabase(new DaSchemaInfoLevel(), physicalSchema, null, null, false, false);
        } catch (IllegalArgumentException exc) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Kludge to catch the exception here if the schema is not found so that we return null, " +
                        "per the contract of this method", exc);
            }
            return null;
        }
    }

    @Override
    public DaTable getTableInfo(String physicalSchema, String tableName) {
        return this.getTableInfo(physicalSchema, tableName, new DaSchemaInfoLevel().setRetrieveTableAndColumnDetails());
    }

    @Override
    public DaTable getTableInfo(String physicalSchema, String tableName, DaSchemaInfoLevel schemaInfoLevel) {
        DaCatalog database = this.getDatabase(schemaInfoLevel, physicalSchema, tableName, null, false, false);

        switch (database.getTables().size()) {
        case 0:
            return null;
        case 1:
            return database.getTables().iterator().next();
        default:
            throw new IllegalArgumentException("Should have only found 0 or 1 tables here for " + physicalSchema + "," +
                    "" + tableName + "; found " + database.getTables().size());
        }
    }

    @Override
    public ImmutableCollection<DaRoutine> getProcedureInfo(String physicalSchema, String procedureName) {
        return getProcedureInfo(physicalSchema, procedureName, new DaSchemaInfoLevel().setRetrieveRoutineDetails(true));
    }

    @Override
    public ImmutableCollection<DaRoutine> getProcedureInfo(String physicalSchema, String procedureName, DaSchemaInfoLevel schemaInfoLevel) {
        schemaInfoLevel.setRetrieveRoutines(true);  // Ensure that this one is populated at minimum
        DaCatalog database = this.getDatabase(schemaInfoLevel, physicalSchema, null, procedureName, false, false);
        return database.getRoutines();
    }

    private void enrichSchemaCrawlerOptions(Connection conn, SchemaCrawlerOptions options, DatabaseSpecificOverrideOptions databaseSpecificOverrideOptions, String schemaName, String tableName,
            String procedureName) {
        this.dbMetadataDialect.customEdits(options, conn, schemaName);

        // 1) Add (?i) to allow for case-insensitive searches for schemas
        // 2) for the inclusion rules, we put .*\\.? in front to account for some of the default prefixes that the
        // dbs include
        // e.g. hsql puts PUBLIC., h2 and iq put the database name (which is unrelated to the schema name)

        if (schemaName != null && this.dbMetadataDialect.getSchemaExpression(schemaName) != null) {
            options.setSchemaInclusionRule(new RegularExpressionInclusionRule(this.dbMetadataDialect.getSchemaExpression(schemaName)));
        }
        if (tableName != null) {
            options.setTableInclusionRule(new RegularExpressionInclusionRule(this.dbMetadataDialect.getTableExpression(schemaName, tableName)));
        }
        if (procedureName != null) {
            options.setRoutineInclusionRule(new RegularExpressionInclusionRule(this.dbMetadataDialect.getRoutineExpression(schemaName, procedureName)));
        }
    }
}
