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
package com.gs.obevo.db.apps.reveng;

import org.apache.commons.lang3.builder.ToStringBuilder;
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

    public RevEngDestination getDestination() {
        return this.destination;
    }

    public String getName() {
        return this.name;
    }

    public String getSql() {
        return this.sql;
    }

    public String getChangeAnnotation() {
        return this.changeAnnotation;
    }

    int getOrder() {
        return this.order;
    }

    MutableList<String> getMetadataAnnotations() {
        return this.metadataAnnotations;
    }

    public void addMetadataAnnotation(String metadataAnnotation) {
        this.metadataAnnotations.add(metadataAnnotation);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("destination", destination)
                .append("name", name)
                .append("changeAnnotation", changeAnnotation)
                .append("order", order)
                .append("metadataAnnotations", metadataAnnotations)
                .append("sql", sql)
                .toString();
    }
}
