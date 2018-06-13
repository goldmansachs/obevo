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
package com.gs.obevo.impl;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeBehavior;
import com.gs.obevo.api.platform.ChangeTypeSemantic;
import com.gs.obevo.api.platform.CommandExecutionContext;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;

/**
 * Registry for the changeTypeBehavior instances for the given environment.
 */
public class ChangeTypeBehaviorRegistry {
    private final ImmutableMap<String, ChangeTypeBehavior> changeTypeBehaviors;
    private final ImmutableMap<String, ChangeTypeSemantic> changeTypeSemantics;

    public static ChangeTypeBehaviorRegistryBuilder newBuilder() {
        return new ChangeTypeBehaviorRegistryBuilder();
    }

    private ChangeTypeBehaviorRegistry(ImmutableMap<String, ChangeTypeBehavior> changeTypeBehaviors, ImmutableMap<String, ChangeTypeSemantic> changeTypeSemantics) {
        this.changeTypeBehaviors = changeTypeBehaviors;
        this.changeTypeSemantics = changeTypeSemantics;
    }

    public ChangeTypeSemantic getChangeTypeSemantic(String changeTypeName) {
        return changeTypeSemantics.get(changeTypeName);
    }

    public ChangeTypeBehavior getChangeTypeBehavior(String changeTypeName) {
        return changeTypeBehaviors.get(changeTypeName);
    }

    public ChangeTypeBehavior getChangeTypeBehavior(ChangeType changeType) {
        return changeTypeBehaviors.get(changeType.getName());
    }

    private ChangeTypeSemantic getChangeTypeSemantic(Change change) {
        return changeTypeSemantics.get(change.getChangeType().getName());
    }

    private ChangeTypeBehavior getChangeTypeBehavior(Change change) {
        return changeTypeBehaviors.get(change.getChangeType().getName());
    }

    public void deploy(Change change, CommandExecutionContext cec) {
        getChangeTypeBehavior(change).deploy(change, cec);
    }

    public void undeploy(Change change) {
        getChangeTypeBehavior(change).undeploy(change);
    }

    public void dropObject(Change change) {
        getChangeTypeBehavior(change).dropObject(change, false);
    }

    public void manage(Change change, ChangeAuditDao changeAuditDao, DeployExecution deployExecution) {
        getChangeTypeSemantic(change).manage(change, changeAuditDao, deployExecution);
    }

    public void unmanage(Change change, ChangeAuditDao changeAuditDao) {
        getChangeTypeSemantic(change).unmanage(change, changeAuditDao);
    }

    public void unmanageObject(Change change, ChangeAuditDao changeAuditDao) {
        getChangeTypeSemantic(change).unmanageObject(change, changeAuditDao);
    }

    public ChangeTypeBehaviorRegistryBuilder toBuilder() {
        return new ChangeTypeBehaviorRegistryBuilder(changeTypeBehaviors.toMap(), changeTypeSemantics.toMap());
    }

    public static class ChangeTypeBehaviorRegistryBuilder {
        private final MutableMap<String, ChangeTypeBehavior> changeTypeBehaviors;
        private final MutableMap<String, ChangeTypeSemantic> changeTypeSemantics;

        private ChangeTypeBehaviorRegistryBuilder() {
            this(Maps.mutable.<String, ChangeTypeBehavior>empty(), Maps.mutable.<String, ChangeTypeSemantic>empty());
        }

        private ChangeTypeBehaviorRegistryBuilder(MutableMap<String, ChangeTypeBehavior> changeTypeBehaviors, MutableMap<String, ChangeTypeSemantic> changeTypeSemantics) {
            this.changeTypeBehaviors = changeTypeBehaviors;
            this.changeTypeSemantics = changeTypeSemantics;
        }

        public ChangeTypeBehaviorRegistryBuilder put(String changeTypeName, ChangeTypeSemantic semantic, ChangeTypeBehavior behavior) {
            putBehavior(changeTypeName, behavior);
            putSemantic(changeTypeName, semantic);
            return this;
        }

        ChangeTypeBehaviorRegistryBuilder putSemantic(String changeTypeName, ChangeTypeSemantic semantic) {
            changeTypeSemantics.put(changeTypeName, semantic);
            return this;
        }

        public ChangeTypeBehaviorRegistryBuilder putBehavior(String changeTypeName, ChangeTypeBehavior behavior) {
            changeTypeBehaviors.put(changeTypeName, behavior);
            return this;
        }

        public ChangeTypeBehaviorRegistry build() {
            return new ChangeTypeBehaviorRegistry(changeTypeBehaviors.toImmutable(), changeTypeSemantics.toImmutable());
        }
    }
}
