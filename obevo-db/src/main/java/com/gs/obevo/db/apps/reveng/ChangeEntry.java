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
package com.gs.obevo.db.apps.reveng;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

public class ChangeEntry {
    private final RevEngDestination destination;
    private final String sql;
    private final String name;
    private final String changeAnnotation;
    private final int order;
    private final MutableList<String> metadataAnnotations = Lists.mutable.empty();

    public ChangeEntry(RevEngDestination destination, String sql) {
        this(destination, sql, null, null, 0);
    }

    public ChangeEntry(RevEngDestination destination, String sql, String name, String changeAnnotation, int order) {
        this.destination = destination;
        this.name = name;
        this.sql = sql;
        this.changeAnnotation = changeAnnotation;
        this.order = order;
    }

    public static final Function<ChangeEntry, RevEngDestination> TO_DESTINATION = new Function<ChangeEntry, RevEngDestination>() {
        @Override
        public RevEngDestination valueOf(ChangeEntry object) {
            return object.getDestination();
        }
    };

    public RevEngDestination getDestination() {
        return this.destination;
    }

    public static final Function<ChangeEntry, String> TO_NAME = new Function<ChangeEntry, String>() {
        @Override
        public String valueOf(ChangeEntry object) {
            return object.getName();
        }
    };

    public String getName() {
        return this.name;
    }

    public static final Function<ChangeEntry, String> TO_SQL = new Function<ChangeEntry, String>() {
        @Override
        public String valueOf(ChangeEntry object) {
            return object.getSql();
        }
    };

    public String getSql() {
        return this.sql;
    }

    public String getChangeAnnotation() {
        return this.changeAnnotation;
    }

    public static final Function<ChangeEntry, Integer> TO_ORDER = new Function<ChangeEntry, Integer>() {
        @Override
        public Integer valueOf(ChangeEntry object) {
            return object.getOrder();
        }
    };

    public int getOrder() {
        return this.order;
    }

    public static final Function<ChangeEntry, MutableList<String>> TO_METADATA_ANNOTATIONS = new Function<ChangeEntry, MutableList<String>>() {
        @Override
        public MutableList<String> valueOf(ChangeEntry object) {
            return object.getMetadataAnnotations();
        }
    };

    public MutableList<String> getMetadataAnnotations() {
        return this.metadataAnnotations;
    }

    public void addMetadataAnnotation(String metadataAnnotation) {
        this.metadataAnnotations.add(metadataAnnotation);
    }
}
