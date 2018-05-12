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
package com.gs.obevo.api.appdata

/**
 * Creating this marker class to separate the ideal schema (e.g. myappschema) from the actual
 * physical schema we may deploy to in dev/qa/prod (e.g. myappschema_qa1). This will span either the catalog or
 * catalog + schema combination depending on the DBMS type.
 */
data class PhysicalSchema @JvmOverloads constructor(
        /**
         * Returns the main schema/catalog name.
         *
         * @since 6.0.0
         */
        val physicalName: String,
        /**
         * Returns the schema/subschema name within the containing catalog. Not applicable for all DBMS types.
         *
         * @since 6.4.0
         */
        val subschema: String? = null
) {
    override fun toString(): String {
        return if (subschema == null) physicalName else "$physicalName.$subschema"
    }

    companion object {
        @JvmStatic
        fun parseFromString(schemaString: String): PhysicalSchema {
            val schemaParts = schemaString.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            when (schemaParts.size) {
                1 -> return PhysicalSchema(schemaParts[0])
                2 -> return PhysicalSchema(schemaParts[0], schemaParts[1])
                else -> throw IllegalArgumentException("Schema string should have at most 1 period in it")
            }
        }
    }
}
