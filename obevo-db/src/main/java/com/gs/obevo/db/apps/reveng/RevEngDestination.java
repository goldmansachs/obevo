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
package com.gs.obevo.db.apps.reveng;

import java.io.File;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DaConstants;
import org.eclipse.collections.api.block.function.Function;

public class RevEngDestination {
    private final String schema;
    private final ChangeType dbObjectType;
    private final String objectName;
    private final boolean duplicate;
    private final boolean baselineEligible;

    public static final Function<RevEngDestination, String> TO_SCHEMA = new Function<RevEngDestination, String>() {
        @Override
        public String valueOf(RevEngDestination revEngDestination) {
            return revEngDestination.getSchema();
        }
    };

    public static final Function<RevEngDestination, String> TO_IDENTITY = new Function<RevEngDestination, String>() {
        @Override
        public String valueOf(RevEngDestination revEngDestination) {
            return revEngDestination.getIdentity();
        }
    };

    public static final Function<RevEngDestination, String> TO_OBJECT_NAME = new Function<RevEngDestination, String>() {
        @Override
        public String valueOf(RevEngDestination revEngDestination) {
            return revEngDestination.getObjectName();
        }
    };

    public static final Function<RevEngDestination, ChangeType> TO_DB_OBJECT_TYPE = new Function<RevEngDestination, ChangeType>() {
        @Override
        public ChangeType valueOf(RevEngDestination revEngDestination) {
            return revEngDestination.getDbObjectType();
        }
    };

    public RevEngDestination(String schema, ChangeType dbObjectType, String objectName, boolean duplicate) {
        this.schema = schema;
        this.dbObjectType = dbObjectType;
        this.objectName = objectName;
        this.duplicate = duplicate;
        this.baselineEligible = !dbObjectType.isRerunnable();
    }

    public String getSchema() {
        return this.schema;
    }

    public File getDestinationFile(File destinationRoot, boolean applyBaseline) {
        File root = new File(destinationRoot, this.schema);
        if (this.dbObjectType != null) {
            String duplicateSuffix = this.duplicate ? "-possibleDuplicateToCleanUp" + DaConstants.ANALYZE_FOLDER_SUFFIX : "";
            root = new File(root, this.dbObjectType.getDirectoryName() + duplicateSuffix);
            if (applyBaseline) {
                root = new File(root, "baseline");
            }
        }
        String fileName = applyBaseline ? this.objectName + ".baseline.sql" : this.objectName + ".sql";
        return new File(root, fileName);
    }

    public String getIdentity() {
        return this.schema + ":" + this.dbObjectType + ":" + this.duplicate + ":" + this.objectName;
    }

    public boolean isBaselineEligible() {
        return this.baselineEligible;
    }

    public ChangeType getDbObjectType() {
        return this.dbObjectType;
    }

    public String getObjectName() {
        return this.objectName;
    }

    public boolean isDuplicate() {
        return this.duplicate;
    }

    @Override
    public String toString() {
        return "RevEngDestination{" +
                "schema='" + schema + '\'' +
                ", dbObjectType=" + dbObjectType +
                ", objectName='" + objectName + '\'' +
                ", duplicate=" + duplicate +
                ", baselineEligible=" + baselineEligible +
                '}';
    }
}
