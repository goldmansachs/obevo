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
package com.gs.obevo.dbmetadata.impl.dialects;

import java.io.StringWriter;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.testutil.TestTemplateUtil;
import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DaForeignKey;
import com.gs.obevo.dbmetadata.api.DaNamedObject;
import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaRule;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DaSequence;
import com.gs.obevo.dbmetadata.api.DaSynonym;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DaUserType;
import com.gs.obevo.dbmetadata.api.DaView;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.dbmetadata.api.RuleBinding;
import org.apache.commons.dbutils.QueryRunner;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.ImmutableMultimap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.test.Verify;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Standard text suite to verify the metadata retrieval.
 */
@Ignore("The child classes should get executed")
public abstract class AbstractDbMetadataManagerIT {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDbMetadataManagerIT.class);

    private final DataSource dataSource;
    private final PhysicalSchema physicalSchema;
    private DbMetadataManager mgr;

    public AbstractDbMetadataManagerIT(DataSource dataSource, PhysicalSchema physicalSchema) {
        this.dataSource = dataSource;
        this.physicalSchema = physicalSchema;
    }

    PhysicalSchema getPhysicalSchema() {
        return physicalSchema;
    }

    final String getSchemaName() {
        return physicalSchema.getPhysicalName();
    }

    String getSubschemaString() {
        return getPhysicalSchema().getSubschema() != null ? getPhysicalSchema().getSubschema() + "." : "";
    }

    private DataSource getDataSource() {
        return dataSource;
    }

    protected abstract DbMetadataManager createMetadataManager();

    protected abstract void setCurrentSchema(QueryRunner jdbc) throws Exception;

    protected abstract String getDropSqlFile();

    protected abstract String getAddSqlFile();

    boolean isPmdKnownBroken() {
        return false;
    }

    protected abstract String convertName(String name);

    @Before
    public void setup() throws Exception {
        DataSource ds = getDataSource();
        mgr = createMetadataManager();
        mgr.setDataSource(ds);

        QueryRunner jdbc = new QueryRunner(ds, isPmdKnownBroken());

        setCurrentSchema(jdbc);

        String subschema = getPhysicalSchema().getSubschema() != null ? getPhysicalSchema().getSubschema() + "." : "";

        StringWriter sw = new StringWriter();
        MutableMap<String, Object> params = Maps.mutable.<String, Object>with("subschema", subschema);

        TestTemplateUtil.getInstance().writeTemplate(getDropSqlFile(), params, sw);
        for (String sql : splitSql(sw.toString())) {
            try {
                LOG.debug("Executing drop: {}", sql);
                jdbc.update(sql);
            } catch (Exception ignore) {
                LOG.info("Ignoring error here for dropping tables (simplest way to do this), {}", ignore.getMessage());
            }
        }

        sw = new StringWriter();
        TestTemplateUtil.getInstance().writeTemplate(getAddSqlFile(), params, sw);
        for (String sql : splitSql(sw.toString())) {
            LOG.debug("Executing update: {}", sql);
            jdbc.update(sql);
        }
    }

    @Test
    public void testGetAllTablesAndViewsAndRoutines() throws Exception {
        DaCatalog database = mgr.getDatabase(getPhysicalSchema(), new DaSchemaInfoLevel().setMaximum(), true, true);
        ImmutableCollection<DaTable> tables = database.getTables();
        Multimap<String, DaTable> tablesByName = tables.groupBy(DaNamedObject.TO_NAME);

        verify_TABLE_A(tablesByName, "TABLE_A");
        verify_TABLE_B_WITH_FK(tablesByName, "TABLE_B_WITH_FK");
        verify_TABLE_A_MULTICOL_PK(tablesByName, "TABLE_A_MULTICOL_PK");
        verify_TABLE_B_WITH_MULTICOL_FK(tablesByName, "TABLE_B_WITH_MULTICOL_FK");
        if (isViewSupported()) {
            verify_VIEW1(tablesByName, "VIEW1");
            if (isInvalidViewPossible()) {
                verify_INVALID_VIEW(tablesByName, "INVALID_VIEW");
            }
        }

        Multimap<String, DaRoutine> routinesByName = database.getRoutines().groupBy(DaNamedObject.TO_NAME);
        if (isStoredProcedureSupported()) {
            verify_SP1(routinesByName, "SP1");
            verify_SP_WITH_OVERLOAD(routinesByName, "SP_WITH_OVERLOAD");
        }

        if (isFunctionSupported()) {
            verify_FUNC1(routinesByName, "FUNC1");
            verify_FUNC_WITH_OVERLOAD(routinesByName, "FUNC_WITH_OVERLOAD");
        }

        if (isSequenceSupported()) {
            ImmutableMultimap<String, DaSequence> sequencesByName = database.getSequences().groupBy(DaNamedObject.TO_NAME);
            verify_REGULAR_SEQUENCE(sequencesByName, "REGULAR_SEQUENCE");
        }

        if (isSynonymSupported()) {
            ImmutableMultimap<String, DaSynonym> synonymsByName = database.getSynonyms().groupBy(DaNamedObject.TO_NAME);

            ImmutableCollection<DaSynonym> synTableA = synonymsByName.get(convertName("SYN_TABLE_A"));
            Verify.assertSize(1, synTableA);
            assertEquals(convertName("SYN_TABLE_A"), synTableA.getFirst().getName());

            ImmutableCollection<DaSynonym> synView1 = synonymsByName.get(convertName("SYN_VIEW1"));
            Verify.assertSize(1, synView1);
            assertEquals(convertName("SYN_VIEW1"), synView1.getFirst().getName());
        }

        if (isRuleSupported()) {
            ImmutableMultimap<String, DaRule> rulesByName = database.getRules().groupBy(DaNamedObject.TO_NAME);
            verify_booleanRule(rulesByName, "booleanRule");
            verify_booleanRule2(rulesByName, "booleanRule2");
        }

        if (isRuleBindingSupported()) {
            ImmutableCollection<RuleBinding> ruleBindings = database.getRuleBindings();
            verifyRuleBindings(ruleBindings);
        }

        if (isUserTypeSupported()) {
            ImmutableMultimap<String, DaUserType> userTypesByName = database.getUserTypes().groupBy(DaNamedObject.TO_NAME);
            verify_MyType(userTypesByName, "MyType");
            verify_MyType2(userTypesByName, "MyType2");
        }
    }

    private void verify_MyType(ImmutableMultimap<String, DaUserType> userTypesByName, String typeName) {
        typeName = convertName(typeName);
        Verify.assertSize(1, userTypesByName.get(typeName));
        DaUserType userType = userTypesByName.get(typeName).toList().get(0);

        assertEquals(typeName, userType.getName());
    }

    private void verify_MyType2(ImmutableMultimap<String, DaUserType> userTypesByName, String typeName) {
        typeName = convertName(typeName);
        Verify.assertSize(1, userTypesByName.get(typeName));
        DaUserType userType = userTypesByName.get(typeName).toList().get(0);

        assertEquals(typeName, userType.getName());
    }

    private void verifyRuleBindings(ImmutableCollection<RuleBinding> ruleBindings) {
        Verify.assertSize(1, ruleBindings);
        RuleBinding ruleBinding = ruleBindings.toList().get(0);
        assertEquals("booleanRule", ruleBinding.getRule());
        assertEquals("TEST_TYPES", ruleBinding.getObject());
        assertEquals("sp_bindrule booleanRule, 'TEST_TYPES.myBooleanCol'", ruleBinding.getSql());
    }

    private void verify_booleanRule(ImmutableMultimap<String, DaRule> rulesByName, String ruleName) {
        ruleName = convertName(ruleName);
        Verify.assertSize(1, rulesByName.get(ruleName));
        DaRule rule = rulesByName.get(ruleName).toList().get(0);

        assertEquals(ruleName, rule.getName());
    }

    private void verify_booleanRule2(ImmutableMultimap<String, DaRule> rulesByName, String ruleName) {
        ruleName = convertName(ruleName);
        Verify.assertSize(1, rulesByName.get(ruleName));
        DaRule rule = rulesByName.get(ruleName).toList().get(0);

        assertEquals(ruleName, rule.getName());
    }

    private void verify_REGULAR_SEQUENCE(ImmutableMultimap<String, DaSequence> sequencesByName, String sequenceName) {
        sequenceName = convertName(sequenceName);
        Verify.assertSize(1, sequencesByName.get(sequenceName));
        DaSequence sequence = sequencesByName.get(sequenceName).toList().get(0);

        assertEquals(sequenceName, sequence.getName());
    }

    @Test
    public void testSpecificTableLookup() throws Exception {
        DaTable table = mgr.getTableInfo(getPhysicalSchema(), "TABLE_A", new DaSchemaInfoLevel().setRetrieveTableAndColumnDetails());
        verify_TABLE_A(table, "TABLE_A");
    }

    @Test
    public void testSpecificRoutineLookup() throws Exception {
        if (isStoredProcedureSupported()) {
            DaRoutine sp = mgr.getRoutineInfo(getPhysicalSchema(), "SP1", new DaSchemaInfoLevel().setRetrieveRoutineDetails(true)).getFirst();
            verify_SP1(sp, "SP1");
        }
        if (isFunctionSupported()) {
            DaRoutine func = mgr.getRoutineInfo(getPhysicalSchema(), "FUNC1", new DaSchemaInfoLevel().setRetrieveRoutineDetails(true)).getFirst();
            verify_FUNC1(func, "FUNC1");
        }
    }

    private void verify_SP_WITH_OVERLOAD(Multimap<String, DaRoutine> routinesByName, String routineName) {
        if (isSpOverloadSupported() == OverLoadSupport.SEPARATE_OBJECT) {
            routineName = convertName(routineName);
            Verify.assertSize(3, routinesByName.get(routineName));
            MutableList<DaRoutine> routines = routinesByName.get(routineName).toList().toSortedListBy(DaRoutine.TO_DEFINITION);

            Verify.assertAllSatisfy(routines, Predicates.attributeEqual(Functions.chain(DaRoutine.TO_SCHEMA, DaSchema.TO_NAME), getSchemaName()));
            Verify.assertAllSatisfy(routines, Predicates.attributeEqual(DaRoutine.TO_ROUTINE_TYPE, DaRoutineType.procedure));

            Verify.assertSize(3, routines.collect(DaRoutine.TO_SPECIFIC_NAME).toSet());

            assertThat(routines.get(0).getDefinition(), equalToIgnoringWhiteSpace(get_SP_WITH_OVERLOAD_1()));
            assertThat(routines.get(1).getDefinition(), equalToIgnoringWhiteSpace(get_SP_WITH_OVERLOAD_2()));
            assertThat(routines.get(2).getDefinition(), equalToIgnoringWhiteSpace(get_SP_WITH_OVERLOAD_3()));
        } else if (isSpOverloadSupported() == OverLoadSupport.NONE) {
            Verify.assertSize(1, routinesByName.get(routineName));
            DaRoutine routine = routinesByName.get(routineName).toList().get(0);

            assertEquals(routineName, routine.getName());
            assertEquals(getSchemaName(), routine.getSchema().getName());
            assertEquals(DaRoutineType.procedure, routine.getRoutineType());
            assertThat(routine.getDefinition(), equalToIgnoringWhiteSpace(get_SP_WITH_OVERLOAD_3()));
            assertThat(routine.getSpecificName(), not(isEmptyString()));
        } else if (isSpOverloadSupported() == OverLoadSupport.COMBINED_OBJECT) {
            Verify.assertSize(1, routinesByName.get(routineName));
            DaRoutine routine = routinesByName.get(routineName).toList().get(0);

            assertEquals(routineName, routine.getName());
            assertEquals(getSchemaName(), routine.getSchema().getName());
            assertEquals(DaRoutineType.procedure, routine.getRoutineType());
            // TODO Fix in GITHUB#7 - this is not yet supported properly
            assertThat(routine.getSpecificName(), not(isEmptyString()));
//            assertThat(routine.getDefinition(), equalToIgnoringWhiteSpace(get_SP_WITH_OVERLOAD_3()));
        }
    }

    private void verify_FUNC_WITH_OVERLOAD(Multimap<String, DaRoutine> routinesByName, String routineName) {
        if (isFuncOverloadSupported()) {
            routineName = convertName(routineName);
            Verify.assertSize(3, routinesByName.get(routineName));
            MutableList<DaRoutine> routines = routinesByName.get(routineName).toList().toSortedListBy(DaRoutine.TO_DEFINITION);

            Verify.assertAllSatisfy(routines, Predicates.attributeEqual(Functions.chain(DaRoutine.TO_SCHEMA, DaSchema.TO_NAME), getSchemaName()));
            if (!isOnlySpSupported()) {
                Verify.assertAllSatisfy(routines, Predicates.attributeEqual(DaRoutine.TO_ROUTINE_TYPE, DaRoutineType.function));
            }

            Verify.assertSize(3, routines.collect(DaRoutine.TO_SPECIFIC_NAME).toSet());

            assertThat(routines.get(0).getDefinition(), equalToIgnoringWhiteSpace(get_FUNC_WITH_OVERLOAD_1()));
            assertThat(routines.get(1).getDefinition(), equalToIgnoringWhiteSpace(get_FUNC_WITH_OVERLOAD_2()));
            assertThat(routines.get(2).getDefinition(), equalToIgnoringWhiteSpace(get_FUNC_WITH_OVERLOAD_3()));
        } else {
            Verify.assertSize(1, routinesByName.get(routineName));
            DaRoutine routine = routinesByName.get(routineName).toList().get(0);

            assertEquals(routineName, routine.getName());
            assertEquals(getSchemaName(), routine.getSchema().getName());
            if (!isOnlySpSupported()) {
                assertEquals(DaRoutineType.function, routine.getRoutineType());
            }
            assertThat(routine.getSpecificName(), not(isEmptyString()));
            assertThat(routine.getDefinition(), equalToIgnoringWhiteSpace(get_FUNC_WITH_OVERLOAD_3()));
        }
    }

    private void verify_SP1(Multimap<String, DaRoutine> routinesByName, String routineName) {
        routineName = convertName(routineName);
        Verify.assertSize(1, routinesByName.get(routineName));
        DaRoutine routine = routinesByName.get(routineName).toList().get(0);
        verify_SP1(routine, routineName);
    }

    private void verify_SP1(DaRoutine routine, String routineName) {
        routineName = convertName(routineName);
        assertEquals(routineName, routine.getName());
        assertEquals(getSchemaName(), routine.getSchema().getName());
        assertEquals(DaRoutineType.procedure, routine.getRoutineType());
        assertThat(routine.getDefinition(), equalToIgnoringWhiteSpace(get_SP1()));
        assertThat(routine.getSpecificName(), not(isEmptyString()));
    }

    private void verify_FUNC1(Multimap<String, DaRoutine> routinesByName, String routineName) {
        routineName = convertName(routineName);
        Verify.assertSize(1, routinesByName.get(routineName));
        DaRoutine routine = routinesByName.get(routineName).toList().get(0);
        verify_FUNC1(routine, routineName);
    }

    private void verify_FUNC1(DaRoutine routine, String routineName) {
        routineName = convertName(routineName);
        assertEquals(routineName, routine.getName());
        assertEquals(getSchemaName(), routine.getSchema().getName());
        if (!isOnlySpSupported()) {
            assertEquals(DaRoutineType.function, routine.getRoutineType());
        }
        assertThat(routine.getDefinition(), equalToIgnoringWhiteSpace(get_FUNC1()));
        assertThat(routine.getSpecificName(), not(isEmptyString()));
    }

    void verify_VIEW1(Multimap<String, DaTable> tablesByName, String tableName) {
        tableName = convertName(tableName);
        Verify.assertSize(1, tablesByName.get(tableName));
        DaView view = (DaView) tablesByName.get(tableName).toList().get(0);

        assertEquals(tableName, view.getName());
        assertEquals(getSchemaName(), view.getSchema().getName());
        assertTrue(view.isView());
        assertThat(view.getDefinition(), equalToIgnoringWhiteSpace(get_VIEW1()));
    }

    private void verify_INVALID_VIEW(Multimap<String, DaTable> tablesByName, String tableName) {
        tableName = convertName(tableName);
        Verify.assertSize(1, tablesByName.get(tableName));
        DaView view = (DaView) tablesByName.get(tableName).toList().get(0);

        assertEquals(tableName, view.getName());
        assertEquals(getSchemaName(), view.getSchema().getName());
        assertTrue(view.isView());
        assertThat(view.getDefinition(), equalToIgnoringWhiteSpace(
                get_INVALID_VIEW()));
        assertNotEquals("Y", view.getAttribute("VALID"));
    }

    private void verify_TABLE_A(Multimap<String, DaTable> tablesByName, String tableName) {
        tableName = convertName(tableName);
        Verify.assertSize(1, tablesByName.get(tableName));
        DaTable table = tablesByName.get(tableName).toList().get(0);
        verify_TABLE_A(table, tableName);
    }

    private void verify_TABLE_A(DaTable table, String tableName) {
        tableName = convertName(tableName);
        assertEquals(tableName, table.getName());
        assertEquals(getSchemaName(), table.getSchema().getName());
        assertFalse(table.isView());

        assertNotNull(table.getPrimaryKey());
        Verify.assertSize(1, table.getPrimaryKey().getColumns());
        assertEquals(convertName("A_ID"), table.getPrimaryKey().getColumns().get(0).getName());

        Verify.assertSize(0, table.getImportedForeignKeys());
    }

    private void verify_TABLE_B_WITH_FK(Multimap<String, DaTable> tablesByName, String tableName) {
        tableName = convertName(tableName);
        Verify.assertSize(1, tablesByName.get(tableName));
        DaTable table = tablesByName.get(tableName).toList().get(0);

        assertEquals(tableName, table.getName());
        assertEquals(getSchemaName(), table.getSchema().getName());
        assertFalse(table.isView());

        assertNotNull(table.getPrimaryKey());
        Verify.assertSize(1, table.getPrimaryKey().getColumns());
        assertEquals(convertName("B_ID"), table.getPrimaryKey().getColumns().get(0).getName());

        Verify.assertSize(1, table.getImportedForeignKeys());
        DaForeignKey fk = table.getImportedForeignKeys().iterator().next();
        assertEquals(convertName("FK_A"), fk.getName());

        Verify.assertSize(1, fk.getColumnReferences());
        assertEquals(convertName("OTHER_A_ID"), fk.getColumnReferences().get(0).getForeignKeyColumn().getName());
        assertEquals(convertName("A_ID"), fk.getColumnReferences().get(0).getPrimaryKeyColumn().getName());
    }

    private void verify_TABLE_A_MULTICOL_PK(Multimap<String, DaTable> tablesByName, String tableName) {
        tableName = convertName(tableName);
        Verify.assertSize(1, tablesByName.get(tableName));
        DaTable table = tablesByName.get(tableName).toList().get(0);

        assertEquals(tableName, table.getName());
        assertEquals(getSchemaName(), table.getSchema().getName());
        assertFalse(table.isView());

        assertNotNull(table.getPrimaryKey());
        Verify.assertSize(2, table.getPrimaryKey().getColumns());
        assertEquals(convertName("A1_ID"), table.getPrimaryKey().getColumns().get(0).getName());
        assertEquals(convertName("A2_ID"), table.getPrimaryKey().getColumns().get(1).getName());

        Verify.assertSize(0, table.getImportedForeignKeys());
    }

    private void verify_TABLE_B_WITH_MULTICOL_FK(Multimap<String, DaTable> tablesByName, String tableName) {
        tableName = convertName(tableName);
        Verify.assertSize(1, tablesByName.get(tableName));
        DaTable table = tablesByName.get(tableName).toList().get(0);

        assertEquals(tableName, table.getName());
        assertEquals(getSchemaName(), table.getSchema().getName());
        assertFalse(table.isView());

        assertNotNull(table.getPrimaryKey());
        Verify.assertSize(1, table.getPrimaryKey().getColumns());
        assertEquals(convertName("B_ID"), table.getPrimaryKey().getColumns().get(0).getName());

        Verify.assertSize(1, table.getImportedForeignKeys());
        DaForeignKey fk = table.getImportedForeignKeys().iterator().next();
        assertEquals(convertName("FK_A_MULTICOL"), fk.getName());

        Verify.assertSize(2, fk.getColumnReferences());
        assertEquals(convertName("OTHER_A1_ID"), fk.getColumnReferences().get(0).getForeignKeyColumn().getName());
        assertEquals(convertName("A1_ID"), fk.getColumnReferences().get(0).getPrimaryKeyColumn().getName());
        assertEquals(convertName("OTHER_A2_ID"), fk.getColumnReferences().get(1).getForeignKeyColumn().getName());
        assertEquals(convertName("A2_ID"), fk.getColumnReferences().get(1).getPrimaryKeyColumn().getName());
    }

    private static String[] splitSql(String sqlContent) throws Exception {
        Pattern splitter = Pattern.compile("(?i)^GO$", Pattern.MULTILINE);
        return splitter.split(sqlContent);
    }

    enum OverLoadSupport {
        NONE,
        COMBINED_OBJECT,
        SEPARATE_OBJECT,;
    }

    boolean isInvalidViewPossible() {
        return true;
    }

    private boolean isViewSupported() {
        return true;
    }

    boolean isFuncOverloadSupported() {
        return true;
    }

    boolean isStoredProcedureSupported() {
        return true;
    }

    boolean isFunctionSupported() {
        return true;
    }

    boolean isOnlySpSupported() {
        return false;
    }

    boolean isSequenceSupported() {
        return true;
    }

    boolean isSynonymSupported() {
        return false;
    }

    OverLoadSupport isSpOverloadSupported() {
        return OverLoadSupport.SEPARATE_OBJECT;
    }

    boolean isRuleBindingSupported() {
        return false;
    }

    boolean isRuleSupported() {
        return false;
    }

    boolean isUserTypeSupported() {
        return false;
    }

    String get_SP_WITH_OVERLOAD_1() {
        throw new UnsupportedOperationException("Please implement me if this test defines this object");
    }

    String get_SP_WITH_OVERLOAD_2() {
        throw new UnsupportedOperationException("Please implement me if this test defines this object");
    }

    String get_SP_WITH_OVERLOAD_3() {
        throw new UnsupportedOperationException("Please implement me if this test defines this object");
    }

    String get_FUNC_WITH_OVERLOAD_1() {
        throw new UnsupportedOperationException("Please implement me if this test defines this object");
    }

    String get_FUNC_WITH_OVERLOAD_2() {
        throw new UnsupportedOperationException("Please implement me if this test defines this object");
    }

    String get_FUNC_WITH_OVERLOAD_3() {
        throw new UnsupportedOperationException("Please implement me if this test defines this object");
    }

    String get_SP1() {
        throw new UnsupportedOperationException("Please implement me if this test defines this object");
    }

    String get_FUNC1() {
        throw new UnsupportedOperationException("Please implement me if this test defines this object");
    }

    String get_VIEW1() {
        throw new UnsupportedOperationException("Please implement me if this test defines this object");
    }

    String get_INVALID_VIEW() {
        throw new UnsupportedOperationException("Please implement me if this test defines this object");
    }
}
