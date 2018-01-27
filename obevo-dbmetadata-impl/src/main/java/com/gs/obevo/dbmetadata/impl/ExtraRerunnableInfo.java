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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.collections.api.block.function.Function;

/**
 * Info object to hold data for Sybase view and routine definitions.
 */
public class ExtraRerunnableInfo {
    private final String name;
    private final String specificName;
    private final String definition;
    private final int order2;
    private final int order1;
    private final String type;

    public ExtraRerunnableInfo(String name, String specificName, String definition) {
        this(name, specificName, definition, null, 0, 0);
    }

    public ExtraRerunnableInfo(String name, String specificName, String definition, String type, int order2, int order1) {
        this.name = name;
        this.specificName = specificName;
        this.definition = definition;
        this.type = type;
        this.order2 = order2;
        this.order1 = order1;
    }

    public static final Function<ExtraRerunnableInfo, String> TO_NAME = new Function<ExtraRerunnableInfo, String>() {
        @Override
        public String valueOf(ExtraRerunnableInfo object) {
            return object.getName();
        }
    };

    public String getName() {
        return name;
    }

    public static final Function<ExtraRerunnableInfo, String> TO_SPECIFIC_NAME = new Function<ExtraRerunnableInfo, String>() {
        @Override
        public String valueOf(ExtraRerunnableInfo object) {
            return object.getSpecificName();
        }
    };

    public String getSpecificName() {
        return specificName;
    }

    public String getDefinition() {
        return definition;
    }

    public static final Function<ExtraRerunnableInfo, Integer> TO_ORDER2 = new Function<ExtraRerunnableInfo, Integer>() {
        @Override
        public Integer valueOf(ExtraRerunnableInfo object) {
            return object.getOrder2();
        }
    };

    private int getOrder2() {
        return order2;
    }

    public static final Function<ExtraRerunnableInfo, Integer> TO_ORDER1 = new Function<ExtraRerunnableInfo, Integer>() {
        @Override
        public Integer valueOf(ExtraRerunnableInfo object) {
            return object.getOrder1();
        }
    };

    private int getOrder1() {
        return order1;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("name", name)
                .append("specificName", specificName)
                .append("definition", definition)
                .append("order2", order2)
                .append("order1", order1)
                .append("type", type)
                .toString();
    }
}
