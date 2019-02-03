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
package com.gs.obevo.hibernate.hibernate3

import com.gs.obevo.hibernate.HibernateSchemaGenerator
import org.hibernate.cfg.Configuration
import org.hibernate.cfg.Environment
import org.hibernate.dialect.Dialect
import org.hibernate.jdbc.util.FormatStyle
import java.io.File
import java.util.Properties

class Hibernate3ConfigSchemaGenerator : HibernateSchemaGenerator<Configuration> {
    override fun writeDdlsToFile(dialectClass: Class<*>, config: Configuration, interimPath: File, delimiter: String) {
        val props = Properties()
        props.setProperty(Environment.DIALECT, dialectClass.name)

        val sqls = config.generateSchemaCreationScript(Dialect.getDialect(props)).asList()
                .map { FormatStyle.DDL.formatter.format(it) }

        interimPath.mkdirs()
        File(interimPath, "hiboutput.ddl").writeText(sqls.joinToString("\n$delimiter\n"))
    }
}
