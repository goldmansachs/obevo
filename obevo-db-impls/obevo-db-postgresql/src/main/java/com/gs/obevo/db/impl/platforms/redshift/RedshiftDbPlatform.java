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
package com.gs.obevo.db.impl.platforms.redshift;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.impl.platforms.postgresql.PostgreSqlDbPlatform;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * Implementation for Amazon Redshift platform, whose dialect is derived from PostgreSQL.
 *
 * There are minor differences between them, including on the driver; hence, the need for a separate platform
 * implementation.
 * <ul>
 *     <li>https://docs.aws.amazon.com/redshift/latest/dg/c_redshift-postgres-jdbc.html</li>
 *     <li>https://docs.aws.amazon.com/redshift/latest/dg/c_redshift-and-postgres-sql.html</li>
 * </ul>
 */
public class RedshiftDbPlatform extends PostgreSqlDbPlatform {
    public RedshiftDbPlatform() {
        super("REDSHIFT");
    }

    @Override
    protected String initializeDefaultDriverClassName() {
        return "com.amazon.redshift.jdbc4.Driver";
    }

    @Override
    protected ImmutableList<ChangeType> initializeChangeTypes() {
        // sequences are not supported
        return super.initializeChangeTypes()
                .reject(changeType -> ChangeType.SEQUENCE_STR.equals(changeType.getName()));
    }
}
