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
package com.gs.obevo.util;

import java.util.Collection;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.factory.Multimaps;

public class CollectionUtil {
    public static <T> T returnOne(RichIterable<? extends T> coll, String message) {
        if (coll.size() > 1) {
            throw new IllegalArgumentException("Expectiong only 1 message in this coll (" + message
                    + "), got this instead: " + coll);
        } else if (coll.size() == 1) {
            return coll.iterator().next();
        } else {
            return null;
        }
    }

    public static <T> T returnOnlyOne(RichIterable<? extends T> coll, String message) {
        if (coll.size() > 1 || coll.size() == 0) {
            throw new IllegalArgumentException("Expectiong only 1 message in this coll (" + message
                    + "), got this instead: " + coll);
        } else {
            return coll.iterator().next();
        }
    }

    public static <T> T returnOne(Collection<? extends T> coll, String message) {
        if (coll.size() > 1) {
            throw new IllegalArgumentException("Expectiong only 1 message in this coll (" + message
                    + "), got this instead: " + coll);
        } else if (coll.size() == 1) {
            return coll.iterator().next();
        } else {
            return null;
        }
    }

    /**
     * Verifies that there aren't multiple elements in the given list that have the same attribute value based on the
     * given input function. If found, then an exception is thrown.
     */
    public static <T> void verifyNoDuplicates(RichIterable<T> list, Function<? super T, ? extends Object> dupeKey, String errorMessage) {
        MutableListMultimap<Object, T> objectsByDupeKey = list.groupBy(dupeKey, Multimaps.mutable.list.<Object, T>empty());

        MutableListMultimap<Object, T> dupeObjectsByDupeKey = objectsByDupeKey.selectKeysMultiValues(new Predicate2<Object, Iterable<T>>() {
            @Override
            public boolean accept(Object ley, Iterable<T> list) {
                return CollectionAdapter.wrapList(list).size() > 1;
            }
        });

        if (dupeObjectsByDupeKey.notEmpty()) {
            String separator = "\n--> ";
            throw new IllegalArgumentException(errorMessage + ";\n" +
                    "Duplicate keys:" + dupeObjectsByDupeKey.keysView().makeString(separator, separator, "") + "]\n" +
                    "From duplicate objects: " + dupeObjectsByDupeKey.valuesView().makeString(separator, separator, ""));
        }
    }
}
