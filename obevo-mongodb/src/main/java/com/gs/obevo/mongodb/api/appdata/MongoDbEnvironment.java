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
package com.gs.obevo.mongodb.api.appdata;

import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.mongodb.impl.MongoDbPlatform;
import com.mongodb.MongoClientURI;

public class MongoDbEnvironment extends Environment<MongoDbPlatform> {
    private String connectionURI;

    @Override
    public void copyFieldsFrom(Environment<MongoDbPlatform> baseEnv) {
        MongoDbEnvironment env = (MongoDbEnvironment) baseEnv;
        super.copyFieldsFrom(baseEnv);
        this.connectionURI = env.connectionURI;
    }

    /**
     * The string to connect to MongoDB, following the syntax mentioned here: {@link MongoClientURI}.
     */
    public void setConnectionURI(String connectionURI) {
        this.connectionURI = connectionURI;
    }

    /**
     * The string to connect to MongoDB, following the syntax mentioned here: {@link MongoClientURI}.
     */
    public String getConnectionURI() {
        return connectionURI;
    }
}
