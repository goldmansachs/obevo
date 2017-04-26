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
package com.gs.obevo.dbmetadata.api;

/**
 * Controls the level of information pulled back for the schema retrieval.
 */
public class DaSchemaInfoLevel {
    private boolean retrieveTables;
    private boolean retrieveTableColumns;
    private boolean retrieveTableForeignKeys;
    private boolean retrieveTableIndexes;
    private boolean retrieveTableCheckConstraints;
    private boolean retrieveViewDetails;
    private boolean retrieveRoutines;
    private boolean retrieveRoutineDetails;
    private boolean retrieveSequences;
    private boolean retrieveRules;
    private boolean retrieveRuleBindings;
    private boolean retrieveUserDefinedColumnDataTypes;  // todo see if it works for us

    public DaSchemaInfoLevel setMaximum() {
        this.setRetrieveAllObjectsMinimum();
        this.setRetrieveTableAndColumnDetails();
        this.setRetrieveViewDetails(true);
        this.setRetrieveRoutineDetails(true);
        return this;
    }

    public DaSchemaInfoLevel setRetrieveAllObjectsMinimum() {
        this.setRetrieveTables(true);
        this.setRetrieveRoutines(true);
        this.setRetrieveUserDefinedColumnDataTypes(true);
        this.setRetrieveSequences(true);
        this.setRetrieveRules(true);
        this.setRetrieveRuleBindings(true);
        return this;
    }

    public DaSchemaInfoLevel setRetrieveTableAndColumnDetails() {
        this.setRetrieveTables(true);
        this.setRetrieveTableColumns(true);
        this.setRetrieveTableCheckConstraints(true);
        this.setRetrieveTableForeignKeys(true);
        this.setRetrieveTableIndexes(true);
        return this;
    }

    public boolean isRetrieveTables() {
        return retrieveTables
                || retrieveTableColumns || retrieveTableForeignKeys || retrieveTableIndexes || retrieveTableCheckConstraints
                || retrieveViewDetails;
    }

    public DaSchemaInfoLevel setRetrieveTables(boolean retrieveTables) {
        this.retrieveTables = retrieveTables;
        return this;
    }

    public boolean isRetrieveTableColumns() {
        // schema crawler requires columns to be retrieved if indices are retrieved
        return retrieveTableColumns || isRetrieveTableIndexes();
    }

    public DaSchemaInfoLevel setRetrieveTableColumns(boolean retrieveTableColumns) {
        this.retrieveTableColumns = retrieveTableColumns;
        return this;
    }

    public boolean isRetrieveTableForeignKeys() {
        return retrieveTableForeignKeys;
    }

    public DaSchemaInfoLevel setRetrieveTableForeignKeys(boolean retrieveTableForeignKeys) {
        this.retrieveTableForeignKeys = retrieveTableForeignKeys;
        return this;
    }

    public boolean isRetrieveTableIndexes() {
        return retrieveTableIndexes;
    }

    public DaSchemaInfoLevel setRetrieveTableIndexes(boolean retrieveTableIndexes) {
        this.retrieveTableIndexes = retrieveTableIndexes;
        return this;
    }

    public boolean isRetrieveTableCheckConstraints() {
        return retrieveTableCheckConstraints;
    }

    public DaSchemaInfoLevel setRetrieveTableCheckConstraints(boolean retrieveTableCheckConstraints) {
        this.retrieveTableCheckConstraints = retrieveTableCheckConstraints;
        return this;
    }

    public boolean isRetrieveViewDetails() {
        return retrieveViewDetails;
    }

    public DaSchemaInfoLevel setRetrieveViewDetails(boolean retrieveViewDetails) {
        this.retrieveViewDetails = retrieveViewDetails;
        return this;
    }

    public boolean isRetrieveRoutines() {
        return retrieveRoutines || retrieveRoutineDetails;
    }

    public DaSchemaInfoLevel setRetrieveRoutines(boolean retrieveRoutines) {
        this.retrieveRoutines = retrieveRoutines;
        return this;
    }

    public boolean isRetrieveRoutineDetails() {
        return retrieveRoutineDetails;
    }

    public DaSchemaInfoLevel setRetrieveRoutineDetails(boolean retrieveRoutineDetails) {
        this.retrieveRoutineDetails = retrieveRoutineDetails;
        return this;
    }

    public boolean isRetrieveSequences() {
        return retrieveSequences;
    }

    public DaSchemaInfoLevel setRetrieveSequences(boolean retrieveSequences) {
        this.retrieveSequences = retrieveSequences;
        return this;
    }

    public boolean isRetrieveRules() {
        return retrieveRules;
    }

    public DaSchemaInfoLevel setRetrieveRules(boolean retrieveRules) {
        this.retrieveRules = retrieveRules;
        return this;
    }

    public boolean isRetrieveRuleBindings() {
        return retrieveRuleBindings;
    }

    public DaSchemaInfoLevel setRetrieveRuleBindings(boolean retrieveRuleBindings) {
        this.retrieveRuleBindings = retrieveRuleBindings;
        return this;
    }

    public boolean isRetrieveUserDefinedColumnDataTypes() {
        return retrieveUserDefinedColumnDataTypes;
    }

    public DaSchemaInfoLevel setRetrieveUserDefinedColumnDataTypes(boolean retrieveUserDefinedColumnDataTypes) {
        this.retrieveUserDefinedColumnDataTypes = retrieveUserDefinedColumnDataTypes;
        return this;
    }
}
