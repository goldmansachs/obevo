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
package com.gs.obevo.mongodb.impl;

import com.gs.obevo.mongodb.api.appdata.MongoDbEnvironment;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

/**
 * Utility to instantiate MongoClient instances for use in Obevo.
 */
public final class MongoClientFactory {
    private static final MongoClientFactory INSTANCE = new MongoClientFactory();

    private MongoClientFactory() {
    }

    public static MongoClientFactory getInstance() {
        return INSTANCE;
    }

    public MongoClient getMongoClient(MongoDbEnvironment env) {
        return getMongoClient(env.getConnectionURI());
    }

    public MongoClient getMongoClient(String mongoClientURI) {
        return getMongoClient(new MongoClientURI(mongoClientURI));
    }

    public MongoClient getMongoClient(MongoClientURI mongoClientURI) {
        return new MongoClient(mongoClientURI);
    }
}
