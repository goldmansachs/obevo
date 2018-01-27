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
package com.gs.obevo.dbmetadata.deepcompare;

import java.util.List;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DeepCompareUtilTest {
    @Test
    public void testCompare() {
        ClassCompareInfo c1 = ClassCompareInfo.newBuilder()
                .setClazz(ParentA.class)
                .setKeyFunction(ParentA.TO_FIELD1)
                .addCompareFunction("field2", ParentA.TO_FIELD2)
                .addCompareFunction("childB", ParentA.TO_CHILD_B)
                .addCollectionComparisonInfo(new CollectionFieldCompareInfo(ChildC.class, ParentA.TO_CHILD_CS))
                .build();
        ClassCompareInfo c2 = ClassCompareInfo.newBuilder()
                .setClazz(ChildB.class)
                .setKeyFunction(ChildB.TO_FIELD1)
                .addCompareFunction("field2", ChildB.TO_FIELD2)
                .build();
        ClassCompareInfo c3 = ClassCompareInfo.newBuilder()
                .setClazz(ChildC.class)
                .setKeyFunction(ChildC.TO_FIELD1)
                .addCompareFunction("field2", ChildC.TO_FIELD2)
                .build();
        DeepCompareUtil util = new DeepCompareUtil(Lists.mutable.with(c1, c2, c3));

        MutableList<ParentA> lefts = Lists.mutable.with(
                new ParentA.ParentABuilder().setField1("leftOnly").createParentA()
                ,
                new ParentA.ParentABuilder().setField1("aMatch").setField2(2).setChildB(new ChildB("bMatch", 100))
                        .setChildCs(Lists.mutable.with(new ChildC("cMatch1", 200), new ChildC("cMatch2", 201)))
                        .createParentA()
                ,
                new ParentA.ParentABuilder()
                        .setField1("childDiff")
                        .setField2(2)
                        .setChildB(new ChildB("bMatch", 100))
                        .setChildCs(
                                Lists.mutable.with(new ChildC("cSame", 200), new ChildC("leftOnly", 201), new ChildC(
                                        "Cdiff", 210))
                        ).createParentA()
        );
        MutableList<ParentA> rights = Lists.mutable.with(
                new ParentA.ParentABuilder().setField1("rightOnly").createParentA()
                ,
                new ParentA.ParentABuilder().setField1("aMatch").setField2(2).setChildB(new ChildB("bMatch", 100))
                        .setChildCs(Lists.mutable.with(new ChildC("cMatch1", 200), new ChildC("cMatch2", 201)))
                        .createParentA()
                ,
                new ParentA.ParentABuilder()
                        .setField1("childDiff")
                        .setField2(2)
                        .setChildB(new ChildB("bMatch", 100))
                        .setChildCs(
                                Lists.mutable.with(new ChildC("cSame", 200), new ChildC("rightOnly", 201),
                                        new ChildC("Cdiff", 220))
                        ).createParentA()
        );
        MutableCollection<CompareBreak> compareBreaks = util.compareCollections(ParentA.class, lefts, rights);
        assertEquals(5, compareBreaks.size());
    }

    static class ParentA {
        private final String field1;
        private final int field2;
        private final ChildB childB;
        private final List<ChildC> childCs;

        static final Function<ParentA, String> TO_FIELD1 = new Function<ParentA, String>() {
            @Override
            public String valueOf(ParentA object) {
                return object.getField1();
            }
        };

        static final Function<ParentA, Integer> TO_FIELD2 = new Function<ParentA, Integer>() {
            @Override
            public Integer valueOf(ParentA object) {
                return object.getField2();
            }
        };

        static final Function<ParentA, ChildB> TO_CHILD_B = new Function<ParentA, ChildB>() {
            @Override
            public ChildB valueOf(ParentA object) {
                return object.getChildB();
            }
        };

        static final Function<ParentA, List<ChildC>> TO_CHILD_CS = new Function<ParentA, List<ChildC>>() {
            @Override
            public List<ChildC> valueOf(ParentA object) {
                return object.getChildCs();
            }
        };

        ParentA(String field1, int field2, ChildB childB, List<ChildC> childCs) {
            this.field1 = field1;
            this.field2 = field2;
            this.childB = childB;
            this.childCs = childCs;
        }

        String getField1() {
            return this.field1;
        }

        int getField2() {
            return this.field2;
        }

        ChildB getChildB() {
            return this.childB;
        }

        List<ChildC> getChildCs() {
            return this.childCs;
        }

        static class ParentABuilder {
            private String field1;
            private int field2;
            private ChildB childB;
            private List<ChildC> childCs;

            ParentABuilder setField1(String field1) {
                this.field1 = field1;
                return this;
            }

            ParentABuilder setField2(int field2) {
                this.field2 = field2;
                return this;
            }

            ParentABuilder setChildB(ChildB childB) {
                this.childB = childB;
                return this;
            }

            ParentABuilder setChildCs(List<ChildC> childCs) {
                this.childCs = childCs;
                return this;
            }

            ParentA createParentA() {
                return new ParentA(this.field1, this.field2, this.childB, this.childCs);
            }
        }
    }

    public static class ChildB {
        private final String field1;
        private final int field2;

        static final Function<ChildB, String> TO_FIELD1 = new Function<ChildB, String>() {
            @Override
            public String valueOf(ChildB object) {
                return object.getField1();
            }
        };

        static final Function<ChildB, Integer> TO_FIELD2 = new Function<ChildB, Integer>() {
            @Override
            public Integer valueOf(ChildB object) {
                return object.getField2();
            }
        };

        ChildB(String field1, int field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        String getField1() {
            return this.field1;
        }

        int getField2() {
            return this.field2;
        }
    }

    static class ChildC {
        private final String field1;
        private final int field2;

        static final Function<ChildC, String> TO_FIELD1 = new Function<ChildC, String>() {
            @Override
            public String valueOf(ChildC object) {
                return object.getField1();
            }
        };

        static final Function<ChildC, Integer> TO_FIELD2 = new Function<ChildC, Integer>() {
            @Override
            public Integer valueOf(ChildC object) {
                return object.getField2();
            }
        };

        ChildC(String field1, int field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        String getField1() {
            return this.field1;
        }

        int getField2() {
            return this.field2;
        }
    }
}
