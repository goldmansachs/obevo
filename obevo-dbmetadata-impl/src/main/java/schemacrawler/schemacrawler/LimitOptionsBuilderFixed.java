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

/**
 * Taken from SchemaCrawler project itself. Can be deleted once we upgrade SchemaCrawer to 16.9.2 and they've
 * incorporated our fix.
 *
 * See https://github.com/schemacrawler/SchemaCrawler/issues/333
 */
package schemacrawler.schemacrawler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import schemacrawler.inclusionrule.ExcludeAll;
import schemacrawler.inclusionrule.IncludeAll;
import schemacrawler.inclusionrule.InclusionRule;
import schemacrawler.inclusionrule.RegularExpressionInclusionRule;
import schemacrawler.schema.RoutineType;

import static schemacrawler.schemacrawler.DatabaseObjectRuleForInclusion.ruleForColumnInclusion;
import static schemacrawler.schemacrawler.DatabaseObjectRuleForInclusion.ruleForRoutineInclusion;
import static schemacrawler.schemacrawler.DatabaseObjectRuleForInclusion.ruleForRoutineParameterInclusion;
import static schemacrawler.schemacrawler.DatabaseObjectRuleForInclusion.ruleForSchemaInclusion;
import static schemacrawler.schemacrawler.DatabaseObjectRuleForInclusion.ruleForSequenceInclusion;
import static schemacrawler.schemacrawler.DatabaseObjectRuleForInclusion.ruleForSynonymInclusion;
import static schemacrawler.schemacrawler.DatabaseObjectRuleForInclusion.ruleForTableInclusion;
import static sf.util.Utility.enumValue;
import static sf.util.Utility.isBlank;

/**
 * SchemaCrawler options builder, to build the immutable options to crawl a
 * schema.
 */
public final class LimitOptionsBuilderFixed implements OptionsBuilder<LimitOptionsBuilderFixed, LimitOptions> {
    private static Collection<RoutineType> allRoutineTypes() {
        return EnumSet.of(RoutineType.procedure, RoutineType.function);
    }

    public static LimitOptionsBuilderFixed builder() {
        return new LimitOptionsBuilderFixed();
    }

    private static Collection<String> defaultTableTypes() {
        return Arrays.asList("BASE TABLE", "TABLE", "VIEW");
    }

    public static LimitOptions newLimitOptions() {
        return builder().toOptions();
    }

    private final Map<DatabaseObjectRuleForInclusion, InclusionRule>
            inclusionRules;
    private String tableNamePattern;
    private Optional<Collection<String>> tableTypes;
    private Optional<Collection<RoutineType>> routineTypes;

    /**
     * Default options.
     */
    private LimitOptionsBuilderFixed() {
        inclusionRules = new EnumMap<>(DatabaseObjectRuleForInclusion.class);

        for (DatabaseObjectRuleForInclusion ruleForInclusion : DatabaseObjectRuleForInclusion.values()) {
            resetToDefault(ruleForInclusion);
        }

        tableTypes = Optional.of(defaultTableTypes());
        routineTypes = Optional.of(allRoutineTypes());
    }

    /**
     * Options from properties.
     *
     * @param config Configuration properties
     */
    @Override
    public LimitOptionsBuilderFixed fromConfig(final Config config) {
        if (config == null) {
            return this;
        }

        for (DatabaseObjectRuleForInclusion ruleForInclusion : DatabaseObjectRuleForInclusion.values()) {
            final InclusionRule inclusionRule = config.getInclusionRuleWithDefault(
                    ruleForInclusion.getIncludePatternProperty(),
                    ruleForInclusion.getExcludePatternProperty(),
                    getDefaultInclusionRule(ruleForInclusion));

            inclusionRules.put(ruleForInclusion, inclusionRule);
        }

        return this;
    }

    @Override
    public LimitOptionsBuilderFixed fromOptions(final LimitOptions options) {
        if (options == null) {
            return this;
        }

        for (DatabaseObjectRuleForInclusion ruleForInclusion : DatabaseObjectRuleForInclusion.values()) {
            inclusionRules.put(ruleForInclusion, options.get(ruleForInclusion));
        }

        tableTypes = Optional.ofNullable(options.getTableTypes());
        tableNamePattern = options.getTableNamePattern();
        routineTypes = Optional.ofNullable(options.getRoutineTypes());

        return this;
    }

    @Override
    public Config toConfig() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LimitOptions toOptions() {
        return new LimitOptions(new EnumMap<>(inclusionRules),
                tableTypes
                        .map(types -> new ArrayList<>(types))
                        .orElse(null),
                tableNamePattern,
                routineTypes
                        .map(types -> types.isEmpty() ? EnumSet.noneOf(RoutineType.class) : EnumSet.copyOf(types))
                        .orElse(null));
    }

    public LimitOptionsBuilderFixed includeAllRoutines() {
        includeRoutines(new IncludeAll());
        return this;
    }

    public LimitOptionsBuilderFixed includeAllSequences() {
        includeSequences(new IncludeAll());
        return this;
    }

    public LimitOptionsBuilderFixed includeAllSynonyms() {
        includeSynonyms(new IncludeAll());
        return this;
    }

    public LimitOptionsBuilderFixed includeColumns(final InclusionRule columnInclusionRule) {
        return include(ruleForColumnInclusion, columnInclusionRule);
    }

    public LimitOptionsBuilderFixed includeColumns(final Pattern columnPattern) {
        return include(ruleForColumnInclusion, columnPattern);
    }

    public LimitOptionsBuilderFixed includeSchemas(final Pattern schemaPattern) {
        return include(ruleForSchemaInclusion, schemaPattern);
    }

    public LimitOptionsBuilderFixed includeTables(final Pattern tablePattern) {
        return include(ruleForTableInclusion, tablePattern);
    }

    public LimitOptionsBuilderFixed includeRoutines(final Pattern routinePattern) {
        return include(ruleForRoutineInclusion, routinePattern);
    }

    public LimitOptionsBuilderFixed includeSequences(final Pattern sequencePattern) {
        return include(ruleForSequenceInclusion, sequencePattern);
    }

    public LimitOptionsBuilderFixed includeSynonyms(final Pattern synonymPattern) {
        return include(ruleForSynonymInclusion, synonymPattern);
    }

    public LimitOptionsBuilderFixed includeRoutineParameters(final InclusionRule routineParameterInclusionRule) {
        return include(ruleForRoutineParameterInclusion,
                routineParameterInclusionRule);
    }

    public LimitOptionsBuilderFixed includeRoutines(final InclusionRule routineInclusionRule) {
        return include(ruleForRoutineInclusion, routineInclusionRule);
    }

    public LimitOptionsBuilderFixed includeSchemas(final InclusionRule schemaInclusionRule) {
        return include(ruleForSchemaInclusion, schemaInclusionRule);
    }

    public LimitOptionsBuilderFixed includeSequences(final InclusionRule sequenceInclusionRule) {
        return include(ruleForSequenceInclusion, sequenceInclusionRule);
    }

    public LimitOptionsBuilderFixed includeSynonyms(final InclusionRule synonymInclusionRule) {
        return include(ruleForSynonymInclusion, synonymInclusionRule);
    }

    public LimitOptionsBuilderFixed includeTables(final InclusionRule tableInclusionRule) {
        return include(ruleForTableInclusion, tableInclusionRule);
    }

    public LimitOptionsBuilderFixed routineTypes(final Collection<RoutineType> routineTypes) {
        if (routineTypes == null) {
            // null signifies include all routine types
            this.routineTypes = Optional.empty();
        } else if (routineTypes.isEmpty()) {
            this.routineTypes = Optional.of(Collections.emptySet());
        } else {
            this.routineTypes = Optional.of(new HashSet<>(routineTypes));
        }
        return this;
    }

    /**
     * Sets routine types from a comma-separated list of routine types.
     *
     * @param routineTypesString Comma-separated list of routine types.
     */
    public LimitOptionsBuilderFixed routineTypes(final String routineTypesString) {
        if (routineTypesString != null) {
            final Collection<RoutineType> routineTypes = new HashSet<>();
            final String[] routineTypeStrings = routineTypesString.split(",");
            if (routineTypeStrings != null && routineTypeStrings.length > 0) {
                for (final String routineTypeString : routineTypeStrings) {
                    final RoutineType routineType =
                            enumValue(routineTypeString.toLowerCase(Locale.ENGLISH),
                                    RoutineType.unknown);
                    routineTypes.add(routineType);
                }
            }
            this.routineTypes = Optional.of(routineTypes);
        } else {
            routineTypes = Optional.empty();
        }
        return this;
    }

    public LimitOptionsBuilderFixed tableNamePattern(final String tableNamePattern) {
        if (isBlank(tableNamePattern)) {
            this.tableNamePattern = null;
        } else {
            this.tableNamePattern = tableNamePattern;
        }
        return this;
    }

    public LimitOptionsBuilderFixed tableTypes(final Collection<String> tableTypes) {
        if (tableTypes == null) {
            this.tableTypes = Optional.empty();
        } else if (tableTypes.isEmpty()) {
            this.tableTypes = Optional.of(Collections.emptySet());
        } else {
            this.tableTypes = Optional.of(new HashSet<>(tableTypes));
        }
        return this;
    }

    /**
     * Sets table types requested for output from a comma-separated list of table
     * types. For example: TABLE,VIEW,SYSTEM_TABLE,GLOBAL TEMPORARY,ALIAS,SYNONYM
     *
     * @param tableTypesString Comma-separated list of table types. Can be null if all supported table
     * types are requested.
     */
    public LimitOptionsBuilderFixed tableTypes(final String tableTypesString) {
        if (tableTypesString != null) {
            final Collection<String> tableTypes;
            tableTypes = new HashSet<>();
            final String[] tableTypeStrings = tableTypesString.split(",");
            if (tableTypeStrings != null && tableTypeStrings.length > 0) {
                for (final String tableTypeString : tableTypeStrings) {
                    tableTypes.add(tableTypeString.trim());
                }
            }
            this.tableTypes = Optional.of(tableTypes);
        } else {
            tableTypes = Optional.empty();
        }

        return this;
    }

    private InclusionRule getDefaultInclusionRule(final DatabaseObjectRuleForInclusion ruleForInclusion) {
        final InclusionRule defaultInclusionRule;
        if (ruleForInclusion.isExcludeByDefault()) {
            defaultInclusionRule = new ExcludeAll();
        } else {
            defaultInclusionRule = new IncludeAll();
        }
        return defaultInclusionRule;
    }

    private void resetToDefault(final DatabaseObjectRuleForInclusion ruleForInclusion) {
        inclusionRules.put(ruleForInclusion,
                getDefaultInclusionRule(ruleForInclusion));
    }

    private LimitOptionsBuilderFixed include(final DatabaseObjectRuleForInclusion ruleForInclusion,
            final Pattern pattern) {
        return include(ruleForInclusion,
                new RegularExpressionInclusionRule(pattern));
    }

    private LimitOptionsBuilderFixed include(final DatabaseObjectRuleForInclusion ruleForInclusion,
            final InclusionRule inclusionRule) {
        if (inclusionRule == null) {
            resetToDefault(ruleForInclusion);
        } else {
            inclusionRules.put(ruleForInclusion, inclusionRule);
        }
        return this;
    }
}
