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
package com.gs.obevo.hibernate.hibernate4;

import java.util.List;

import com.gs.obevo.hibernate.HibernateReveng;
import com.gs.obevo.hibernate.HibernateRevengFactory;
import com.gs.obevo.hibernate.HibernateRevengTest;

public class Hibernate4RevengTest extends HibernateRevengTest {
    @Override
    protected HibernateReveng<List<? extends Class<?>>> getHibReveng() {
        return HibernateRevengFactory.getInstance().getHibernate4();
    }
}
