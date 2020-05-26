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
package com.gs.obevo.dbmetadata.impl.dialects;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import schemacrawler.schemacrawler.DatabaseServerType;
import schemacrawler.tools.databaseconnector.DatabaseConnector;
import schemacrawler.tools.executable.commandline.PluginCommand;
import schemacrawler.tools.iosource.ClasspathInputResource;

/**
 * H2 Database Connector. We mainly need one that uses defaults. The UnknownDatabaseConnector default class doesn't
 * work as it is using the arguments from DB2 for the informationSchemaViewsBuilder instead of a generic blank value.
 */
public final class H2DatabaseConnector extends DatabaseConnector {
    private static final long serialVersionUID = 1786572065393663455L;

    public H2DatabaseConnector() throws IOException {
        super(new DatabaseServerType("h2", "H2 DataBase"),
                new ClasspathInputResource("/schemacrawler-h2.config.properties"),
                (informationSchemaViewsBuilder, connection) -> {});
    }

    @Override
    public PluginCommand getHelpCommand() {
        final PluginCommand pluginCommand = super.getHelpCommand();
        pluginCommand
                .addOption("server",
                        "--server=h2%n"
                                + "Loads SchemaCrawler plug-in for H2",
                        String.class)
                .addOption("host",
                        "Host name%n" + "Optional, defaults to localhost",
                        String.class)
                .addOption("port",
                        "Port number%n" + "Optional, defaults to 9001",
                        Integer.class)
                .addOption("database", "Database name", String.class);
        return pluginCommand;
    }

    @Override
    protected Predicate<String> supportsUrlPredicate() {
        return url -> Pattern.matches("jdbc:h2:.*", url);
    }
}
