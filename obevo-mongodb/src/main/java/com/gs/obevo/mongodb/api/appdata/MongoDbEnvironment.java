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
package com.gs.obevo.mongodb.api.appdata;

import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.mongodb.impl.MongoDbPlatform;

public class MongoDbEnvironment extends Environment<MongoDbPlatform> {
    private String host;
    private int port;

    @Override
    public void copyFieldsFrom(Environment<MongoDbPlatform> baseEnv) {
        MongoDbEnvironment env = (MongoDbEnvironment) baseEnv;
        super.copyFieldsFrom(baseEnv);
        this.host = env.host;
        this.port = env.port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
