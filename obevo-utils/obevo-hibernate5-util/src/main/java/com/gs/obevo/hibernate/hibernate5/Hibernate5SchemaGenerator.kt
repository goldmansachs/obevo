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
package com.gs.obevo.hibernate.hibernate5

import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.boot.spi.MetadataImplementor
import org.hibernate.tool.hbm2ddl.SchemaExport
import org.hibernate.tool.hbm2ddl.SchemaExport.Action
import org.hibernate.tool.schema.TargetType
import java.io.File
import java.util.EnumSet

class Hibernate5SchemaGenerator : com.gs.obevo.hibernate.HibernateSchemaGenerator<List<Class<*>>> {
    override fun writeDdlsToFile(dialectClass: Class<*>, config: List<Class<*>>, interimPath: File, delimiter: String) {
        val metadata = MetadataSources(StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", dialectClass.name)
                .build())
        config.forEach { annotatedClass -> metadata.addAnnotatedClass(annotatedClass) }

        val metadataImplementor = metadata.buildMetadata() as MetadataImplementor
        val export = SchemaExport()

        val tempFile = File(interimPath, "hiboutput.ddl")
        tempFile.delete()
        export.setOutputFile(tempFile.absolutePath)
        export.setDelimiter(delimiter)
        export.setFormat(true)

        interimPath.mkdirs()
        export.execute(EnumSet.of(TargetType.SCRIPT), Action.CREATE, metadataImplementor)
    }
}
