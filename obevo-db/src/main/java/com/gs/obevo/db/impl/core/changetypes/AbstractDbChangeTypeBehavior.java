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
package com.gs.obevo.db.impl.core.changetypes;

import java.sql.Connection;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Permission;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.DbChangeTypeBehavior;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.util.MultiLineStringSplitter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for DB platform ChangeTypes that interact via SQL w/ the database and database objects.
 * I mention that wording in case there are other objects or other ways we may want to interact w/ the DB (e.g. file
 * load).
 */
public abstract class AbstractDbChangeTypeBehavior implements DbChangeTypeBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDbChangeTypeBehavior.class);

    private final DbEnvironment env;
    private final DbChangeType dbChangeType;
    private final SqlExecutor sqlExecutor;
    private final DbSimpleArtifactDeployer baseArtifactDeployer;
    private final GrantChangeParser grantChangeParser;

    public AbstractDbChangeTypeBehavior(DbEnvironment env, DbChangeType dbChangeType, SqlExecutor sqlExecutor, DbSimpleArtifactDeployer baseArtifactDeployer, GrantChangeParser grantChangeParser) {
        this.env = env;
        this.dbChangeType = dbChangeType;
        this.sqlExecutor = sqlExecutor;
        this.baseArtifactDeployer = baseArtifactDeployer;
        this.grantChangeParser = grantChangeParser;
    }

    @Override
    public void deploy(final Change change) {
        sqlExecutor.executeWithinContext(change.getPhysicalSchema(), new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                baseArtifactDeployer.deployArtifact(conn, change);
                if (shouldApplyGrants(change)) {
                    ImmutableList<Permission> permsToApply = env.getPermissions().select(
                            Predicates.attributePredicate(PERMISSION_TO_SCHEME,
                                    StringPredicates.equalsIgnoreCase(change.getPermissionScheme())));

                    applyGrants(conn, change.getPhysicalSchema(), change.getObjectName(), permsToApply);
                }
            }
        });
    }


    @Override
    public void unmanage(Change change, ChangeAuditDao changeAuditDao) {
        changeAuditDao.deleteChange(change);
    }

    @Override
    public void unmanageObject(Change change, ChangeAuditDao changeAuditDao) {
        changeAuditDao.deleteObjectChanges(change);
    }

    protected abstract boolean shouldApplyGrants(Change artifact);

    private static final Function<Permission, String> PERMISSION_TO_SCHEME = new Function<Permission, String>() {
        @Override
        public String valueOf(Permission object) {
            return object.getScheme();
        }
    };

    @Override
    public void applyGrants(Connection conn, PhysicalSchema schema, String objectName, RichIterable<Permission> permsToApply) {
        Pair<Boolean, RichIterable<String>> qualifiedObjectNames = getQualifiedObjectNames(conn, schema, objectName);
        ImmutableList<String> grants = this.grantChangeParser.generateGrantChanges(permsToApply, dbChangeType, objectName, qualifiedObjectNames.getTwo(), qualifiedObjectNames.getOne());

        LOG.info(String.format("Applying grants on db object [%s]: found %d total SQL statements to apply",
                objectName, grants.size()));

        for (String grant : grants) {
            if (!StringUtils.isBlank(grant)) {
                // need to check for blank in case it gets tokenized away during the in-memory conversion
                LOG.debug("Executing grant: {}", grant);
                // grants are automatically included as part of the original change, so we don't track the deployment in the
                // audit table
                sqlExecutor.getJdbcTemplate().update(conn, grant);
            }
        }
    }

    @Override
    public void dropObject(final Change change, final boolean dropForRecreate) {
        sqlExecutor.executeWithinContext(change.getPhysicalSchema(), new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                String dropSql = null;
                try {
                    dropSql = getDropSql(conn, change);
                    LOG.info("Dropping object with SQL: {}", dropSql);
                    for (String drop : MultiLineStringSplitter.createSplitterOnSpaceAndLine("GO").valueOf(dropSql)) {
                        if (!StringUtils.isBlank(drop)) {
                            sqlExecutor.getJdbcTemplate().update(conn, drop);
                        }
                    }
                } catch (RuntimeException exc) {
                    if (dropForRecreate) {
                        // TODO See DEPLOYANY-174 - we should detect if the object exists first before trying to drop it and ignoring exceptions if unsuccessful.
                        LOG.debug("Change type {} for Object {} is being deployed anew as this sql did not execute: {}", change.getChangeType(), change.getObjectName(), dropSql);
                    } else {
                        throw exc;
                    }
                }
            }
        });
    }

    /**
     * Returns the qualified object names (e.g. for PostgreSql where a function name corresponds to multiple
     * function overloads, each with their own specific names)
     */
    protected Pair<Boolean, RichIterable<String>> getQualifiedObjectNames(Connection conn, PhysicalSchema physicalSchema, String objectName) {
        return Tuples.<Boolean, RichIterable<String>>pair(false, Lists.immutable.with(objectName));
    }

    private String getDropSql(Connection conn, Change change) {
        String dropContent = change.getDropContent();
        if (dropContent == null) {
            return generateDropChangeRaw(conn, change);
        } else {
            LOG.info("Dropping the object {} using the custom command from the SQL metadata: {}", change.getDisplayString(), dropContent);
            return dropContent;
        }
    }

    protected String generateDropChangeRaw(Connection conn, Change change) {
        String defaultObjectKeyword = dbChangeType.getDefaultObjectKeyword();
        if (defaultObjectKeyword == null) {
            return "";
        } else {
            return "DROP " + defaultObjectKeyword + " " + change.getObjectName();
        }
    }

    protected DbSimpleArtifactDeployer getBaseArtifactDeployer() {
        return baseArtifactDeployer;
    }

    protected SqlExecutor getSqlExecutor() {
        return sqlExecutor;
    }

    protected DbChangeType getDbChangeType() {
        return dbChangeType;
    }
}
