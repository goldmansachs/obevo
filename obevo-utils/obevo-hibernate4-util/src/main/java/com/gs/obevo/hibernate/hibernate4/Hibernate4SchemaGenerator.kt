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
package com.gs.obevo.hibernate.hibernate4

import com.gs.obevo.hibernate.HibernateSchemaGenerator
import org.hibernate.cfg.Configuration
import java.io.File

/**
 * Referenced by reflection by [com.gs.obevo.hibernate.HibernateRevengFactory].
 */
@Suppress("unused")
class Hibernate4SchemaGenerator : HibernateSchemaGenerator<List<Class<*>>> {
    private val baseSchemaGenerator = Hibernate4ConfigSchemaGenerator()
    override fun writeDdlsToFile(dialectClass: Class<*>, modelClasses: List<Class<*>>, interimPath: File, delimiter: String) {
        val hibConfig = Configuration()

        modelClasses.forEach { annotatedClass -> hibConfig.addAnnotatedClass(annotatedClass) }
        hibConfig.buildMappings()
        org.hibernate.envers.configuration.spi.AuditConfiguration.getFor(hibConfig)

        baseSchemaGenerator.writeDdlsToFile(dialectClass, hibConfig, interimPath, delimiter)
    }
}
