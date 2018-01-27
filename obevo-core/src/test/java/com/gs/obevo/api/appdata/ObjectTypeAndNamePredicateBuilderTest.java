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
package com.gs.obevo.api.appdata;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.tuple.Tuples;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObjectTypeAndNamePredicateBuilderTest {
    @Test
    public void testStringParsing() {
        ObjectTypeAndNamePredicateBuilder parse = ObjectTypeAndNamePredicateBuilder.parse("TABLE~tab1,tab2;-VIEW~view1", null);
        Predicates<? super Pair<String, String>> predicate = parse.build(Functions.<String>firstOfPair(), (Function<Pair<String, String>, String>) (Function) Functions.<String>secondOfPair());

        assertTrue(predicate.accept(Tuples.pair("OTHER", "otherInclude")));
        assertTrue(predicate.accept(Tuples.pair("TABLE", "tab1")));
        assertTrue(predicate.accept(Tuples.pair("TABLE", "tab2")));
        assertFalse(predicate.accept(Tuples.pair("TABLE", "tabNo")));
        assertFalse(predicate.accept(Tuples.pair("VIEW", "view1")));
        assertTrue(predicate.accept(Tuples.pair("VIEW", "viewInclude")));
    }

    @Test
    public void testStringParsingWithExclusionDefault() {
        ObjectTypeAndNamePredicateBuilder parse = ObjectTypeAndNamePredicateBuilder.parse("TABLE~tab1,tab2;VIEW~view1", ObjectTypeAndNamePredicateBuilder.FilterType.EXCLUDE);
        Predicates<? super Pair<String, String>> predicate = parse.build(Functions.<String>firstOfPair(), (Function<Pair<String, String>, String>) (Function) Functions.<String>secondOfPair());

        assertTrue(predicate.accept(Tuples.pair("OTHER", "otherInclude")));
        assertFalse(predicate.accept(Tuples.pair("TABLE", "tab1")));
        assertFalse(predicate.accept(Tuples.pair("TABLE", "tab2")));
        assertTrue(predicate.accept(Tuples.pair("TABLE", "tabNo")));
        assertFalse(predicate.accept(Tuples.pair("VIEW", "view1")));
        assertTrue(predicate.accept(Tuples.pair("VIEW", "viewInclude")));
    }
}