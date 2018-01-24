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
package com.gs.obevo.db.impl.platforms;

import java.sql.Connection;

import com.gs.obevo.db.api.platform.DbTranslationDialect;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.impl.PrepareDbChange;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDbTranslationDialect implements DbTranslationDialect {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDbTranslationDialect.class);
    @Override
    public ImmutableList<String> getInitSqls() {
        return Lists.immutable.of();
    }

    @Override
    public ImmutableList<PrepareDbChange> getAdditionalTranslators() {
        return Lists.immutable.of();
    }

    @Override
    public void initSchema(JdbcHelper jdbc, Connection conn) {
    }

    @Override
    public ImmutableSet<String> getDisabledChangeTypeNames() {
        return Sets.immutable.empty();
    }

    protected void updateAndIgnoreException(Connection conn, JdbcHelper jdbc, String sql) {
        try {
            jdbc.update(conn, sql);
        } catch (Exception exc) {
            LOG.trace("Failed to execute SQL; ignoring and continuing as we don't have a way yet to detect if the type already exists (a bit hacky...)", exc);
        }
    }
}
