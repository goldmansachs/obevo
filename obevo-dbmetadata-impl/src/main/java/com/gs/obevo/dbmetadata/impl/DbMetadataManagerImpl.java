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
package com.gs.obevo.dbmetadata.impl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.Level;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DaDirectory;
import com.gs.obevo.dbmetadata.api.DaExtension;
import com.gs.obevo.dbmetadata.api.DaPackage;
import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaRule;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DaUserType;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.dbmetadata.api.RuleBinding;
import com.gs.obevo.util.VisibleForTesting;
import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.crawl.SchemaCrawler;
import schemacrawler.inclusionrule.ExcludeAll;
import schemacrawler.inclusionrule.IncludeAll;
import schemacrawler.inclusionrule.RegularExpressionInclusionRule;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Schema;
import schemacrawler.schemacrawler.InformationSchemaKey;
import schemacrawler.schemacrawler.LimitOptionsBuilderFixed;
import schemacrawler.schemacrawler.LoadOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.schemacrawler.SchemaRetrievalOptions;
import schemacrawler.schemacrawler.SchemaRetrievalOptionsBuilder;

public class DbMetadataManagerImpl implements DbMetadataManager {
    private static final Logger LOG = LoggerFactory.getLogger(DbMetadataManagerImpl.class);

    private final DbMetadataDialect dbMetadataDialect;
    private DataSource ds;

    protected DbMetadataManagerImpl(DbMetadataDialect dbMetadataDialect) {
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
    public DaCatalog getDatabase(PhysicalSchema physicalSchema, DaSchemaInfoLevel schemaInfoLevel, boolean searchAllTables,
            boolean searchAllRoutines) {
        return this.getDatabase(physicalSchema, schemaInfoLevel, searchAllTables, searchAllRoutines, null, null);
    }

    /**
     * We have some special logic in here to interact w/ Schema crawler, as the different RDBMS types have
     * different parameters to the JDBC metadata
     *
     * @param searchAllProcedures @return
     */
    private DaCatalog getDatabase(PhysicalSchema physicalSchema, DaSchemaInfoLevel schemaInfoLevel, boolean searchAllTables, boolean searchAllProcedures, String tableName,
            String procedureName) {
        // Many of the DB metadata drivers like IQ/ASE/DB2 don't support the function metadata lookups and
        // schemacrawler complains (though the library still does the job). We set the log level here to avoid
        // excessive log messages
        java.util.logging.Logger thisLogger = java.util.logging.Logger.getLogger("schemacrawler");
        for (Handler handler : thisLogger.getHandlers()) {
            thisLogger.removeHandler(handler);
        }
        thisLogger.setLevel(Level.SEVERE);

        Validate.notNull(physicalSchema, "physicalSchema must be specified");

        try (Connection conn = this.ds.getConnection()) {
            this.dbMetadataDialect.setSchemaOnConnection(conn, physicalSchema);

            SchemaCrawlerOptionsBuilder options = SchemaCrawlerOptionsBuilder.builder();

            // Set what details are required in the schema - this affects the time taken to crawl the schema
            // Standard works for our use cases (e.g. columns, indices, pks)
            SchemaInfoLevelBuilder schemaInfoLevelBuilder = toInfoLevelBuilder(schemaInfoLevel);
            this.dbMetadataDialect.updateSchemaInfoLevelBuilder(schemaInfoLevelBuilder);
            options.withLoadOptions(LoadOptionsBuilder.builder().withSchemaInfoLevel(schemaInfoLevelBuilder.toOptions()).toOptions());

            SchemaRetrievalOptionsBuilder dbSpecificOptionsBuilder = dbMetadataDialect.getDbSpecificOptionsBuilder(conn, physicalSchema, searchAllTables);
            MutableMap<InformationSchemaKey, String> infoSchemaSqlOverrides = dbMetadataDialect.getInfoSchemaSqlOverrides(physicalSchema);
            if (infoSchemaSqlOverrides != null) {
                MutableMap<String, String> convertedInfoSchemaSqlOverrides = infoSchemaSqlOverrides.collect((key, value) -> Tuples.pair(key.getLookupKey(), value));
                dbSpecificOptionsBuilder.withInformationSchemaViews(convertedInfoSchemaSqlOverrides);
            }
            SchemaRetrievalOptions dbSpecificOptions = dbSpecificOptionsBuilder.toOptions();

            LimitOptionsBuilderFixed limitOptions = LimitOptionsBuilderFixed.builder();
            this.dbMetadataDialect.updateLimitOptionsBuilder(limitOptions);

            String schemaExpression = Objects.requireNonNull(this.dbMetadataDialect.getSchemaExpression(physicalSchema), "Schema expression was not returned for schema " + physicalSchema + " by " + dbMetadataDialect);
            limitOptions.includeSchemas(new RegularExpressionInclusionRule(schemaExpression));

            if (tableName != null) {
                limitOptions.includeTables(new RegularExpressionInclusionRule(this.dbMetadataDialect.getTableExpression(physicalSchema, tableName)));
            } else if (searchAllTables) {
                limitOptions.includeTables(new IncludeAll());
            } else {
                limitOptions.includeTables(new ExcludeAll());
            }

            if (procedureName != null) {
                limitOptions.includeRoutines(new RegularExpressionInclusionRule(this.dbMetadataDialect.getRoutineExpression(physicalSchema, procedureName)));
            } else if (searchAllProcedures) {
                limitOptions.includeRoutines(new IncludeAll());
            } else {
                limitOptions.includeRoutines(new ExcludeAll());
            }

            if (schemaInfoLevel.isRetrieveSequences()) {
                limitOptions.includeSequences(new IncludeAll());
            }
            if (schemaInfoLevel.isRetrieveSynonyms()) {
                limitOptions.includeSynonyms(new IncludeAll());
            }

            options.withLimitOptions(limitOptions.toOptions());

            LOG.debug("Starting query for DB metadata for {}/{}/{}/{}", tableName, procedureName,
                    searchAllTables ? "searching all tables" : "", searchAllProcedures ? "searching all procedures" : "");

            final Catalog database;
            try {
                final SchemaCrawler schemaCrawler = new SchemaCrawler(conn, dbSpecificOptions, options.toOptions());
                database = schemaCrawler.crawl();
            } catch (SchemaCrawlerException e) {
                throw new IllegalArgumentException("Could not lookup schema " + physicalSchema + ": " + e.getMessage(), e);
            }

            LOG.debug("Ending query for DB metadata for {}/{}/{}/{}", tableName, procedureName,
                    searchAllTables ? "searching all tables" : "", searchAllProcedures ? "searching all procedures" : "");

            this.dbMetadataDialect.validateDatabase(database, physicalSchema);

            SchemaStrategy schemaStrategy = dbMetadataDialect.getSchemaStrategy();
            Schema schemaReference = database.getSchemas().isEmpty() ? null : database.getSchemas().iterator().next();
            DaSchema schema = new DaSchemaImpl(schemaReference, schemaStrategy);

            ImmutableCollection<DaRoutine> extraRoutines = Lists.immutable.empty();

            if (schemaInfoLevel.isRetrieveRoutines() && (searchAllProcedures || procedureName != null)) {
                extraRoutines = this.dbMetadataDialect.searchExtraRoutines(schema, procedureName, conn);
            }

            ImmutableCollection<ExtraIndexInfo> extraConstraintIndices = schemaInfoLevel.isRetrieveTableCheckConstraints()
                    ? dbMetadataDialect.searchExtraConstraintIndices(schema, tableName, conn)
                    : Lists.immutable.empty();
            Multimap<String, ExtraIndexInfo> constraintIndices = extraConstraintIndices.groupBy(ExtraIndexInfo::getTableName);

            ImmutableCollection<ExtraRerunnableInfo> extraViewInfo = schemaInfoLevel.isRetrieveViewDetails()
                    ? dbMetadataDialect.searchExtraViewInfo(schema, tableName, conn)
                    : Lists.immutable.empty();

            DaRoutineType routineOverrideValue = dbMetadataDialect.getRoutineOverrideValue();

            ImmutableCollection<RuleBinding> ruleBindings = schemaInfoLevel.isRetrieveRuleBindings()
                    ? dbMetadataDialect.getRuleBindings(schema, conn)
                    : Lists.immutable.empty();
            ImmutableCollection<DaRule> rules = schemaInfoLevel.isRetrieveRules()
                    ? dbMetadataDialect.searchRules(schema, conn)
                    : Lists.immutable.empty();
            ImmutableCollection<DaUserType> userTypes = schemaInfoLevel.isRetrieveUserDefinedColumnDataTypes()
                    ? dbMetadataDialect.searchUserTypes(schema, conn)
                    : Lists.immutable.empty();
            ImmutableCollection<DaPackage> packages = schemaInfoLevel.isRetrieveRoutines()
                    ? dbMetadataDialect.searchPackages(schema, procedureName, conn)
                    : Lists.immutable.empty();

            return new DaCatalogImpl(database, schemaStrategy, userTypes, rules, ruleBindings, extraRoutines, constraintIndices, extraViewInfo, routineOverrideValue, packages);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SchemaInfoLevelBuilder toInfoLevelBuilder(DaSchemaInfoLevel schemaInfoLevel) {
        SchemaInfoLevelBuilder otherInfoLevel = SchemaInfoLevelBuilder.builder();

        otherInfoLevel.setRetrieveDatabaseInfo(true);
        //otherInfoLevel.setRetrieveJdbcDriverInfo(false);  // would prefer to add this back due to issues w/ Sybase ASE; requires followup w/ SchemaCrawler team
        otherInfoLevel.setRetrieveAdditionalJdbcDriverInfo(false);  // unneeded for our use cases and causes some problems w/ some JDBC drivers

        // tables
        otherInfoLevel.setRetrieveTables(schemaInfoLevel.isRetrieveTables());
        otherInfoLevel.setRetrieveAdditionalTableAttributes(schemaInfoLevel.isRetrieveTables());

        // table columns
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
//        otherInfoLevel.setRetrieveRoutineColumns(schemaInfoLevel.isRetrieveRoutineDetails());  // deprecated in SchemaCrawler 16

        // sequences
        otherInfoLevel.setRetrieveSequenceInformation(schemaInfoLevel.isRetrieveSequences());

        // user types
        otherInfoLevel.setRetrieveUserDefinedColumnDataTypes(schemaInfoLevel.isRetrieveUserDefinedColumnDataTypes());  // TODO see if this takes care of domains
        // otherInfoLevel.setRetrieveColumnDataTypes();  // note - setRetrieveColumnDataTypes will query all supported data types, including primitives. This is an expensive operation for some DBMS types. We can avoid it for now

        // table trigger
        otherInfoLevel.setRetrieveTriggerInformation(false);  // will implement this later

        // synonyms
        otherInfoLevel.setRetrieveSynonymInformation(schemaInfoLevel.isRetrieveSynonyms());

        // not yet pulling in privileges; may choose to do this bulk (i.e. flag for privileges pulls in privileges for all object types
        otherInfoLevel.setRetrieveTablePrivileges(false);
        otherInfoLevel.setRetrieveTableColumnPrivileges(false);

        return otherInfoLevel;
    }

    @Override
    public DaCatalog getDatabaseOptional(String physicalSchema) {
        return getDatabaseOptional(new PhysicalSchema(physicalSchema));
    }

    @Override
    public DaCatalog getDatabaseOptional(PhysicalSchema physicalSchema) {
        try {
            return this.getDatabase(physicalSchema, new DaSchemaInfoLevel(), false, false, null, null);
        } catch (RuntimeException exc) {
            return null;
        }
    }

    @Override
    public DaTable getTableInfo(PhysicalSchema physicalSchema, String tableName) {
        return this.getTableInfo(physicalSchema, tableName, new DaSchemaInfoLevel().setRetrieveTables(true));
    }

    @Override
    public DaTable getTableInfo(PhysicalSchema physicalSchema, String tableName, DaSchemaInfoLevel schemaInfoLevel) {
        DaCatalog database = this.getDatabase(physicalSchema, schemaInfoLevel, false, false, tableName, null);

        switch (database.getTables().size()) {
        case 0:
            return null;
        case 1:
            return database.getTables().iterator().next();
        default:
            throw new IllegalArgumentException("Should have only found 0 or 1 tables here for " + physicalSchema + "," +
                    "" + tableName + "; found " + database.getTables().size() + ": " + database.getTables());
        }
    }

    @Override
    public ImmutableCollection<DaRoutine> getRoutineInfo(PhysicalSchema physicalSchema, String routineName) {
        return getRoutineInfo(physicalSchema, routineName, new DaSchemaInfoLevel().setRetrieveRoutineDetails(true));
    }

    @Override
    public ImmutableCollection<DaRoutine> getRoutineInfo(PhysicalSchema physicalSchema, String routineName, DaSchemaInfoLevel schemaInfoLevel) {
        schemaInfoLevel.setRetrieveRoutines(true);  // Ensure that this one is populated at minimum
        DaCatalog database = this.getDatabase(physicalSchema, schemaInfoLevel, false, false, null, routineName);
        return database.getRoutines();
    }

    @Override
    public ImmutableSet<String> getGroupNamesOptional(PhysicalSchema physicalSchema) {
        try (Connection conn = ds.getConnection()) {
            return this.dbMetadataDialect.getGroupNamesOptional(conn, physicalSchema);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ImmutableSet<String> getUserNamesOptional(PhysicalSchema physicalSchema) {
        try (Connection conn = ds.getConnection()) {
            return this.dbMetadataDialect.getUserNamesOptional(conn, physicalSchema);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ImmutableSet<DaDirectory> getDirectoriesOptional() {
        try (Connection conn = ds.getConnection()) {
            return this.dbMetadataDialect.getDirectoriesOptional(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ImmutableSet<DaExtension> getExtensionsOptional() {
        try (Connection conn = ds.getConnection()) {
            return this.dbMetadataDialect.getExtensionsOptional(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
