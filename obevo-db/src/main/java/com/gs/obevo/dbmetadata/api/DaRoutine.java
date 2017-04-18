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
package com.gs.obevo.dbmetadata.api;

import org.eclipse.collections.api.block.function.Function;

public interface DaRoutine extends DaNamedObject, DaDatabaseObject {
    Function<DaRoutine, DaRoutineType> TO_ROUTINE_TYPE = new Function<DaRoutine, DaRoutineType>() {
        @Override
        public DaRoutineType valueOf(DaRoutine object) {
            return object.getRoutineType();
        }
    };
    Function<DaRoutine, String> TO_SPECIFIC_NAME = new Function<DaRoutine, String>() {
        @Override
        public String valueOf(DaRoutine object) {
            return object.getSpecificName();
        }
    };
    Function<DaRoutine, String> TO_DEFINITION = new Function<DaRoutine, String>() {
        @Override
        public String valueOf(DaRoutine object) {
            return object.getDefinition();
        }
    };

    String getSpecificName();

    String getDefinition();

    DaRoutineType getRoutineType();
}
