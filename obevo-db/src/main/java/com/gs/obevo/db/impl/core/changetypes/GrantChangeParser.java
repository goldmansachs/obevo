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

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Grant;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.api.appdata.Permission;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.impl.PrepareDbChange;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrantChangeParser {
    private static final Logger LOG = LoggerFactory.getLogger(GrantChangeParser.class);

    private final DbEnvironment env;
    private final ImmutableList<PrepareDbChange> artifactTranslators;

    public GrantChangeParser(DbEnvironment env, ImmutableList<PrepareDbChange> artifactTranslators) {
        this.env = env;
        this.artifactTranslators = artifactTranslators;
    }

    ImmutableList<String> generateGrantChanges(RichIterable<Permission> permsToApply, final DbChangeType changeType, final PhysicalSchema physicalSchema, final String mainObjectName, RichIterable<String> objectNames, final boolean specific) {
        final MutableList<String> changes = Lists.mutable.empty();

        for (Permission perm : permsToApply) {
            for (final Grant grant : perm.getGrants()) {
                grant.validate();

                for (final String objectName : objectNames) {
                    grant.getGrantTargets().forEachKeyValue(new Procedure2<GrantTargetType, String>() {
                        @Override
                        public void value(GrantTargetType grantTargetType, String grantTarget) {
                            for (String privilege : grant.getPrivileges()) {
                                changes.add(createGrant(env, privilege, changeType, physicalSchema, objectName, grantTargetType, grantTarget, specific));
                            }
                        }
                    });
                }
            }
        }

        if (LOG.isInfoEnabled()) {
            LOG.info(String.format("Applying grants on [%s] with [%d] permission entries on these qualified object names: [%s]",
                    mainObjectName, permsToApply.size(), objectNames.makeString("; ")));
        }

        return changes.toImmutable();
    }

    private String createGrant(DbEnvironment env, String grant,
            ChangeType changeType, PhysicalSchema physicalSchema, String objectName,
            GrantTargetType grantTargetType, String grantTarget, boolean specific) {
        String content = generateGrantSql(env.getPlatform(), grant, changeType, env.getPlatform().getSubschemaPrefix(physicalSchema) + objectName,
                grantTargetType, grantTarget, specific);

        for (PrepareDbChange artifactTranslator : this.artifactTranslators) {
            content = artifactTranslator.prepare(content, null, env);
        }
        return content;
    }

    private static String generateGrantSql(DbPlatform dialect, String grant, ChangeType objectType, String objectName,
            GrantTargetType grantTargetType, String grantTarget, boolean specific) {
        return String.format("GRANT %1$s ON %6$s %2$s %3$s TO %4$s %5$s", grant,
                ((DbChangeType) objectType).getGrantObjectQualifier(), objectName,
                dialect.getGrantTargetTypeStr(grantTargetType, grantTarget), grantTarget,
                specific ? "SPECIFIC" : ""
        );
    }
}
