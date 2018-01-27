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
package com.gs.obevo.impl.changetypes;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DaConstants;

/**
 * Defines a generic "unclassified" ChangeType. This mainly serves a purpose during reverse-engineering, i.e. if we
 * cannot classify some input.
 *
 * This should not be used in a production scenario.
 */
public class UnclassifiedChangeType implements ChangeType {
    public static final UnclassifiedChangeType INSTANCE = new UnclassifiedChangeType();

    @Override
    public String getName() {
        return UNCLASSIFIED_STR;
    }

    @Override
    public boolean isRerunnable() {
        return true;  // have unclassified be rerunnable as this is simpler to handle for the reverse-engineering case
    }

    @Override
    public int getDeployOrderPriority() {
        return 0;
    }

    @Override
    public String getDirectoryName() {
        return "unableToClassify" + DaConstants.ANALYZE_FOLDER_SUFFIX;
    }

    @Override
    public String getDirectoryNameOld() {
        return null;
    }

    @Override
    public boolean isEnrichableForDependenciesInText() {
        return false;
    }

    @Override
    public boolean isDependentObjectRecalculationRequired() {
        return false;
    }

    @Override
    public ChangeType getBodyChangeType() {
        return null;
    }
}
