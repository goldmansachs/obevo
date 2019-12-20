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
package com.gs.obevo.mongodb.impl

import com.gs.obevo.api.platform.Platform
import com.gs.obevo.apps.reveng.AquaRevengArgs
import com.gs.obevo.apps.reveng.ChangeEntry
import com.gs.obevo.apps.reveng.RevEngDestination
import com.gs.obevo.apps.reveng.Reveng
import com.gs.obevo.apps.reveng.RevengWriter
import org.bson.Document
import org.slf4j.LoggerFactory

class MongoDbReveng internal constructor(private val platform: Platform) : Reveng {
    override fun reveng(args: AquaRevengArgs) {
        val mongoClient = MongoClientFactory.getInstance().getMongoClient(args.dbHost, args.dbPort)

        val database = mongoClient.getDatabase(args.dbSchema)

        val collections = database.listCollectionNames()

        val collectionType = platform.getChangeType(MongoDbPlatform.CHANGE_TYPE_COLLECTION)

        val changeEntries = collections.flatMap { collectionName ->
            val collection = database.getCollection(collectionName)

            var counter = 0
            collection.listIndexes()
                    .filterNot { it.getString("name").equals("_id_") }
                    .map { index ->
                        println("INDEX $index")
                        val indexName = index.getString("name")

                        val options = index.filterKeys {
                            !(it.equals("key", ignoreCase = true) || it.equals("v", ignoreCase = true) || it.equals("ns", ignoreCase = true))
                        }

                        val key = (index["key"] as Document).toJson()

                        val sb = StringBuilder()
                        sb.append("db.$collectionName.createIndex(")
                        sb.append("\n\t" + key)
                        if (options.isNotEmpty()) {
                            sb.append("\n\t, " + Document(options).toJson())
                        }
                        sb.append("\n);")

                        ChangeEntry(
                                RevEngDestination(args.dbSchema, collectionType, collectionName, false),
                                sb.toString(),
                                indexName,
                                "INDEX",
                                counter++
                        )
                    }
        }

        RevengWriter().write(platform, changeEntries, args.outputPath, args.isGenerateBaseline, RevengWriter.defaultShouldOverwritePredicate(), args.excludeObjects)
        RevengWriter().writeConfig(
                "com/gs/obevo/mongodb/system-config-template.xml.ftl",
                platform,
                args.outputPath,
                listOf(args.dbSchema),
                mapOf("host" to args.dbHost, "port" to args.dbPort.toString())
        )
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MongoDbReveng::class.java)
    }
}
