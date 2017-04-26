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
package com.gs.obevo.impl.changesorter;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.impl.ExecuteChangeCommand;
import com.gs.obevo.impl.graph.SortableDependency;
import com.gs.obevo.impl.graph.SortableDependencyGroup;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * Represents a node in the graph to be sorted.
 */
class DbCommandSortKey implements SortableDependencyGroup {
    private final ExecuteChangeCommand changeCommand;
    private final boolean drop;
    private final ChangeType changeType;
    private final String objectName;
    private final int orderWithinObject;
    private int order;

    public static final Function<ExecuteChangeCommand, DbCommandSortKey> CREATE = new Function<ExecuteChangeCommand, DbCommandSortKey>() {
        @Override
        public DbCommandSortKey valueOf(ExecuteChangeCommand changeCommand) {
            return new DbCommandSortKey(changeCommand);
        }
    };

    public DbCommandSortKey(ExecuteChangeCommand changeCommand) {
        this.changeCommand = changeCommand;
        this.drop = changeCommand.isDrop();
        final Change candidateChange = changeCommand.getChanges().getFirst();
        this.changeType = candidateChange.getChangeType();
        this.objectName = candidateChange.getObjectName();
        this.orderWithinObject = candidateChange.getOrderWithinObject();
    }


    public static final Function<DbCommandSortKey, ExecuteChangeCommand> TO_CHANGE_COMMAND = new Function<DbCommandSortKey, ExecuteChangeCommand>() {
        @Override
        public ExecuteChangeCommand valueOf(DbCommandSortKey object) {
            return object.getChangeCommand();
        }
    };

    public ExecuteChangeCommand getChangeCommand() {
        return changeCommand;
    }

    public static final Function<DbCommandSortKey, Boolean> TO_DROP = new Function<DbCommandSortKey, Boolean>() {
        @Override
        public Boolean valueOf(DbCommandSortKey object) {
            return object.isDrop();
        }
    };

    public boolean isDrop() {
        return this.drop;
    }

    public static final Function<DbCommandSortKey, ChangeType> TO_CHANGE_TYPE = new Function<DbCommandSortKey,
            ChangeType>() {
        @Override
        public ChangeType valueOf(DbCommandSortKey object) {
            return object.getChangeType();
        }
    };

    public ChangeType getChangeType() {
        return this.changeType;
    }

    public static final Function<DbCommandSortKey, String> TO_OBJECT_NAME = new Function<DbCommandSortKey, String>() {
        @Override
        public String valueOf(DbCommandSortKey object) {
            return object.getObjectName();
        }
    };

    public String getObjectName() {
        return this.objectName;
    }

    public static final Function<DbCommandSortKey, Integer> TO_ORDER = new Function<DbCommandSortKey,
            Integer>() {
        @Override
        public Integer valueOf(DbCommandSortKey object) {
            return object.getOrder();
        }
    };

    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public String toString() {
        return "DbCommandSortKey{" +
                "changeCommand=" + changeCommand +
                ", drop=" + drop +
                ", changeType=" + changeType +
                ", objectName='" + objectName + '\'' +
                ", orderWithinObject=" + orderWithinObject +
                ", order=" + order +
                '}';
    }

    @Override
    public ImmutableSet<SortableDependency> getComponents() {
        return (ImmutableSet) changeCommand.getChanges().toSet().toImmutable();
    }
}
