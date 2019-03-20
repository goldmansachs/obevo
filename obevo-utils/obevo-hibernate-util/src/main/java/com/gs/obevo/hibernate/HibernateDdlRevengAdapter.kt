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
package com.gs.obevo.hibernate

import com.gs.obevo.api.platform.ChangeType
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng
import com.gs.obevo.db.apps.reveng.AquaRevengArgs
import com.gs.obevo.impl.util.MultiLineStringSplitter
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.impl.factory.Lists
import java.io.File
import java.io.PrintStream

internal class HibernateDdlRevengAdapter<in T>(
        private val hibSchGen: HibernateSchemaGenerator<T>,
        private val revengArgs: HibernateRevengArgs<T>)
    : AbstractDdlReveng(revengArgs.platform, MultiLineStringSplitter(DELIMITER, false), Lists.immutable.empty(), getRevengPatterns(revengArgs), null) {

    init {
        setStartQuote(QUOTE)
        setEndQuote(QUOTE)
    }

    override fun doRevengOrInstructions(out: PrintStream, args: AquaRevengArgs, interimDir: File): Boolean {
        interimDir.mkdirs()

        hibSchGen.writeDdlsToFile(this.revengArgs.hibernateDialectClass, this.revengArgs.getConfig(), interimDir, DELIMITER)
        return true
    }

    companion object {
        private val QUOTE = ""
        private val DELIMITER = ";"

        private fun getRevengPatterns(revengArgs: HibernateRevengArgs<*>): ImmutableList<AbstractDdlReveng.RevengPattern> {
            val schemaNameSubPattern: String
            val namePatternType: AbstractDdlReveng.NamePatternType

            if (revengArgs.platform.isSubschemaSupported) {
                schemaNameSubPattern = AbstractDdlReveng.getCatalogSchemaObjectPattern(QUOTE, QUOTE)
                namePatternType = AbstractDdlReveng.NamePatternType.THREE
            } else {
                schemaNameSubPattern = AbstractDdlReveng.getSchemaObjectPattern(QUOTE, QUOTE)
                namePatternType = AbstractDdlReveng.NamePatternType.TWO
            }

            val remapObjectName = { objectName: String ->
                if (objectName.endsWith("_AUD"))
                    objectName.replace(Regex("_AUD$"), "")
                else
                    objectName
            }

            return Lists.immutable.with(
                    RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+table\\s+$schemaNameSubPattern")
                            .withPostProcessSql { LineParseOutput(it + (revengArgs.postCreateTableSql ?: "")) }
                            .withRemapObjectName(remapObjectName),
                    RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)create\\s+(?:\\w+\\s+)?index\\s+$schemaNameSubPattern\\s+on\\s+$schemaNameSubPattern", 2, 1, "INDEX")
                            .withRemapObjectName(remapObjectName),
                    RevengPattern(ChangeType.TABLE_STR, namePatternType, "(?i)alter\\s+table\\s+$schemaNameSubPattern\\s+add\\s+constraint\\s+$schemaNameSubPattern\\s+foreign\\s+key", 1, 2, "FK").withShouldBeIgnored(!revengArgs.isGenerateForeignKeys)
                            .withRemapObjectName(remapObjectName),
                    RevengPattern(ChangeType.SEQUENCE_STR, namePatternType, "(?i)create\\s+sequence\\s+$schemaNameSubPattern\\s+")
                            .withRemapObjectName(remapObjectName)
            )
        }
    }
}
