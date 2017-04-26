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
package com.gs.obevo.db.impl.core.cleaner;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeType;
import org.eclipse.collections.api.block.function.Function;

public class DbCleanCommand {
    private final PhysicalSchema physicalSchema;
    private final ChangeType objectType;
    private final String objectName;
    private final String sqlStatement;

    public static final Function<DbCleanCommand, String> TO_KEY = new Function<DbCleanCommand,
            String>() {
        @Override
        public String valueOf(DbCleanCommand arg0) {
            return arg0.getPhysicalSchema().getPhysicalName() + ":" + arg0.getObjectType() + ":" + arg0.getObjectName();
        }
    };

    public DbCleanCommand(PhysicalSchema physicalSchema, ChangeType objectType, String objectName) {
        this(physicalSchema, objectType, objectName, null);
    }

    /**
     * This overload is only for tables (the drop SQL is not needed otherwise)
     */
    public DbCleanCommand(PhysicalSchema physicalSchema, ChangeType objectType, String objectName, String sqlStatement) {
        this.physicalSchema = physicalSchema;
        this.objectType = objectType;
        this.objectName = objectName;
        this.sqlStatement = sqlStatement;
    }

    public static final Function<DbCleanCommand, ChangeType> TO_OBJECT_TYPE = new Function<DbCleanCommand, ChangeType>() {
        @Override
        public ChangeType valueOf(DbCleanCommand object) {
            return object.getObjectType();
        }
    };
    public ChangeType getObjectType() {
        return this.objectType;
    }

    public static final Function<DbCleanCommand, String> TO_OBJECT_NAME = new Function<DbCleanCommand, String>() {
        @Override
        public String valueOf(DbCleanCommand object) {
            return object.getObjectName();
        }
    };
    public String getObjectName() {
        return this.objectName;
    }

    /**
     * Only needed for incremental change types
     */
    public String getSqlStatement() {
        return this.sqlStatement;
    }

    public PhysicalSchema getPhysicalSchema() {
        return this.physicalSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DbCleanCommand)) {
            return false;
        }

        DbCleanCommand that = (DbCleanCommand) o;

        if (!this.objectName.equals(that.objectName)) {
            return false;
        }
        if (this.objectType != that.objectType) {
            return false;
        }
        if (this.sqlStatement == null ? that.sqlStatement != null : !this.sqlStatement.equals(that.sqlStatement)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = this.objectType.hashCode();
        result = 31 * result + this.objectName.hashCode();
        result = 31 * result + (this.sqlStatement == null ? 0 : this.sqlStatement.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "DbCleanCommand{" +
                "physicalSchema=" + physicalSchema +
                ", objtypeEnum=" + objectType +
                ", objname='" + objectName + '\'' +
                ", sqlstatement='" + sqlStatement + '\'' +
                '}';
    }
}
