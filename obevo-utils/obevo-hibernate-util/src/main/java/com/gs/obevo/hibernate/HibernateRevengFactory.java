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
package com.gs.obevo.hibernate;

import java.util.List;

public class HibernateRevengFactory {
    private static final HibernateRevengFactory INSTANCE = new HibernateRevengFactory();

    public static HibernateRevengFactory getInstance() {
        return INSTANCE;
    }

    public HibernateReveng<List<? extends Class<?>>> getHibernate3() {
        return new HibernateRevengImpl<>(getDefaultSchemaGenerator("3"));
    }

    public HibernateReveng<List<? extends Class<?>>> getHibernate4() {
        return new HibernateRevengImpl<>(getDefaultSchemaGenerator("4"));
    }

    public HibernateReveng<List<? extends Class<?>>> getHibernate5() {
        return new HibernateRevengImpl<>(getDefaultSchemaGenerator("5"));
    }

    private HibernateSchemaGenerator<List<? extends Class<?>>> getDefaultSchemaGenerator(String version) {
        String className = "com.gs.obevo.hibernate.hibernate" + version + ".Hibernate" + version + "SchemaGenerator";
        try {
            return (HibernateSchemaGenerator<List<? extends Class<?>>>) Class.forName(className).newInstance();
        } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
