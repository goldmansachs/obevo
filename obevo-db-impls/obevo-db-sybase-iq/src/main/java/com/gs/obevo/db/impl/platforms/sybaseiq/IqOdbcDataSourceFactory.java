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
package com.gs.obevo.db.impl.platforms.sybaseiq;

import java.sql.Driver;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.util.inputreader.Credential;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

/**
 * ODBC driver data source factory.
 * See here for the driver history - http://scn.sap.com/community/sql-anywhere/blog/2014/05/02/connecting-to-sql-anywhere-using-jdbc
 */
public class IqOdbcDataSourceFactory extends AbstractIqDataSourceFactory {
    @Override
    public boolean isDriverAccepted(Class<? extends Driver> driverClass) {
        return driverClass.getName().startsWith("sybase.jdbc");
    }

    @Override
    protected Pair<String, String> getUrl(DbEnvironment env, String schema, Credential credential) {
        String url = "jdbc:sqlanywhere:" +
                "ServerName=" + env.getDbServer() + "" +
                ";LINKS=TCPIP{host=" + env.getDbHost() + ":" + env.getDbPort() + "}" +
                "";
        return Tuples.pair(url, credential.getPassword());
    }

    @Override
    public boolean isIqClientLoadSupported() {
        return true;
    }
}
