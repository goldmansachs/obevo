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
package com.gs.obevo.schemagen;

import java.io.File;
import java.util.Random;

import com.gs.obevo.db.testutil.TestTemplateUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction0;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.primitive.IntInterval;

/**
 * Utility to generate a large complex schema for the kata example.
 */
public class SchemaGenerator {

    public static final Function<String, String> CONVERT_NAME_FUNCTION = StringFunctions.toUpperCase();

    public static void main(String[] args) {
        new SchemaGenerator().generate("MYLARGESCHEMA");
    }

    private final int numTypes = 10;
    private final int numTables = 500;
    private final int numViews = 100;
    private final int numViewGroups = 4;
    private final int numProcs = 1000;
    private final int numProcGroups = 4;

    // types used by tables
    // tables used by views and procs
    // views used by views, procs
    // procs used by procs
    private final MinMaxSupplier tableToTypesRatio = getMinMaxSupplier(0, 3, 1, 2);
    private final MinMaxSupplier viewToTablesRatio = getMinMaxSupplier(1, 3, 1, 2);
    private final MinMaxSupplier viewToViewsRatio = getMinMaxSupplier(0, 3, 1, 2);
    private final MinMaxSupplier spToTablesRatio = getMinMaxSupplier(0, 3, 1, 2);
    private final MinMaxSupplier spToViewsRatio = getMinMaxSupplier(0, 3, 1, 2);
    private final MinMaxSupplier spToSpsRatio = getMinMaxSupplier(1, 4, 1, 2);

    private final Random rand = new Random();

    public void generate(String schema) {
        MutableSet<MyInput> inputs = Sets.mutable.empty();

        inputs.withAll(getUserTypes(numTypes));
        inputs.withAll(getTables());
        inputs.withAll(getViews());
        inputs.withAll(getSps());


        MutableSet<String> types = Sets.mutable.of("table", "view", "sp", "usertype");
        File outputDir = new File("./target/testoutput");
        FileUtils.deleteQuietly(outputDir);
        outputDir.mkdirs();
        for (MyInput input : inputs) {
            MutableMap<String, Object> params = Maps.mutable.<String, Object>empty().withKeyValue(
                    "name", input.getName()
            );
            for (String type : types) {
                params.put("dependent" + type + "s", input.getDependenciesByType().get(type));
            }

            File outputFile = new File(outputDir, schema + "/" + input.getType() + "/" + input.getName() + ".sql");
            outputFile.getParentFile().mkdirs();
            TestTemplateUtil.getInstance().writeTemplate("schemagen/" + input.getType() + ".sql.ftl", params, outputFile);
        }

    }

    private ImmutableList<MyInput> getUserTypes(int numTypes) {
        return IntInterval.fromTo(0, numTypes - 1).collect(new IntToObjectFunction<MyInput>() {
            @Override
            public MyInput valueOf(int i) {
                return new MyInput("usertype" + i, "usertype", Multimaps.mutable.list.<String, String>empty(), StringFunctions.toUpperCase());
            }
        });
    }

    private ImmutableList<MyInput> getTables() {
        return IntInterval.fromTo(0, numTables - 1).collect(new IntToObjectFunction<MyInput>() {
            @Override
            public MyInput valueOf(int i) {
                MutableSetMultimap<String, String> params = Multimaps.mutable.set.empty();

                int tableNumTypes = tableToTypesRatio.getValue();
                for (int depIndex = 0; depIndex < tableNumTypes; depIndex++) {
                    int depNum = rand.nextInt(numTypes);
                    params.put("usertype", "usertype" + depNum);
                }

                return new MyInput("table" + i, "table", params, CONVERT_NAME_FUNCTION);
            }
        });
    }

    private ImmutableList<MyInput> getViews() {
        final int[] viewGroupMapping = new int[numViews];
        final MutableListMultimap<Integer, Integer> viewGroups = Multimaps.mutable.list.empty();
        IntInterval.fromTo(0, numViews - 1).each(new IntProcedure() {
            @Override
            public void value(int viewNum) {
                int viewGroupNum = rand.nextInt(numViewGroups);
                viewGroupMapping[viewNum] = viewGroupNum;
                for (int v = 0; v < viewGroupNum; v++) {
                    viewGroups.put(v, viewNum);
                }
            }
        });
        return IntInterval.fromTo(0, numViews - 1).collect(new IntToObjectFunction<MyInput>() {
            @Override
            public MyInput valueOf(int viewNum) {
                MutableSetMultimap<String, String> params = Multimaps.mutable.set.<String, String>empty();

                int tableNumTypes = viewToTablesRatio.getValue();
                for (int depIndex = 0; depIndex < tableNumTypes; depIndex++) {
                    int depNum = rand.nextInt(numTables);
                    params.put("table", "table" + depNum);
                }

                int viewNumTypes = viewToViewsRatio.getValue();

                // aiming to avoid circular dependencies, so we split views into groups to ensure that one group can only depend on another in an acyclical manner
                MutableList<Integer> potentialViewDependencies = viewGroups.get(viewGroupMapping[viewNum]);
                for (int depIndex = 0; depIndex < viewNumTypes && depIndex < potentialViewDependencies.size(); depIndex++) {
                    int depNum = viewNum;
                    while (depNum == viewNum) {
                        depNum = potentialViewDependencies.get(rand.nextInt(potentialViewDependencies.size()));
                    }
                    params.put("view", "view" + depNum);
                }

                return new MyInput("view" + viewNum, "view", params, StringFunctions.toUpperCase());
            }
        });
    }

    private ImmutableList<MyInput> getSps() {
        final int[] spGroupMapping = new int[numProcs];
        final MutableListMultimap<Integer, Integer> spGroups = Multimaps.mutable.list.empty();
        IntInterval.fromTo(0, numProcs - 1).each(new IntProcedure() {
            @Override
            public void value(int spNum) {
                int spGroupNum = rand.nextInt(numProcGroups);
                spGroupMapping[spNum] = spGroupNum;
                if (spGroupNum != 0) {
                    spGroups.put(spGroupNum - 1, spNum);
                }
            }
        });
        return IntInterval.fromTo(0, numProcs - 1).collect(new IntToObjectFunction<MyInput>() {
            @Override
            public MyInput valueOf(int spNum) {
                MutableSetMultimap<String, String> params = Multimaps.mutable.set.<String, String>empty();

                int tableNumTypes = spToTablesRatio.getValue();
                for (int depIndex = 0; depIndex < tableNumTypes; depIndex++) {
                    int depNum = rand.nextInt(numTables);
                    params.put("table", "table" + depNum);
                }

                int viewNumTypes = spToViewsRatio.getValue();
                for (int depIndex = 0; depIndex < viewNumTypes; depIndex++) {
                    int depNum = rand.nextInt(numViews);
                    params.put("view", "view" + depNum);
                }


                // aiming to avoid circular dependencies, so we split SPs into groups to ensure that one group can only depend on another in an acyclical manner
                MutableList<Integer> potentialSpDependencies = spGroups.get(spGroupMapping[spNum]);
                int spNumTypes = spToSpsRatio.getValue();
                for (int depIndex = 0; depIndex < spNumTypes && depIndex < potentialSpDependencies.size(); depIndex++) {
                    int depNum = spNum;
                    while (depNum == spNum) {
                        depNum = potentialSpDependencies.get(rand.nextInt(potentialSpDependencies.size()));
                    }

                    params.put("sp", "sp" + depNum);
                }

                return new MyInput("sp" + spNum, "sp", params, StringFunctions.toUpperCase());
            }
        });
    }

    public static class MyInput {
        private final String name;
        private final String type;
        private final Multimap<String, String> dependenciesByType;

        public MyInput(String name, String type, Multimap<String, String> dependenciesByType, Function<String, String> convertNameFunction) {
            this.name = convertNameFunction.valueOf(name);
            this.type = type;
            this.dependenciesByType = dependenciesByType.collectValues(StringFunctions.toUpperCase());
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public Multimap<String, String> getDependenciesByType() {
            return dependenciesByType;
        }
    }

    private MinMaxSupplier getMinMaxSupplier(int min, int max, int mean, int sd) {
        final NormalDistribution distribution = new NormalDistribution(mean, sd);
        return new MinMaxSupplier(new DoubleFunction0() {
            @Override
            public double value() {
                return distribution.sample();
            }
        }, min, max);
    }

    public static class MinMaxSupplier {
        private final DoubleFunction0 supplier;
        private final int min;
        private final int max;

        public MinMaxSupplier(DoubleFunction0 supplier, int min, int max) {
            this.supplier = supplier;
            this.min = min;
            this.max = max;
        }

        public int getValue() {
            long sampleValue = Math.round(supplier.value());
            sampleValue = Math.min(sampleValue, min);
            sampleValue = Math.max(sampleValue, max);
            return (int) sampleValue;
        }
    }
}
