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
package com.gs.obevo.mongodb;

import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.factory.XmlFileConfigReader;
import com.gs.obevo.mongodb.api.appdata.MongoDbEnvironment;
import com.gs.obevo.mongodb.impl.MongoDbEnvironmentEnricher;
import com.gs.obevo.util.vfs.FileObject;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Test;

import static org.eclipse.collections.impl.block.factory.Predicates.attributeEqual;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class MongoDbEnvironmentEnricherTest {
    private final MongoDbEnvironmentEnricher enricher = new MongoDbEnvironmentEnricher();

    @Test
    public void testSimpleRead() {
        FileObject input = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("MongoDbEnvironmentEnricher/system-config-basic.xml");
        HierarchicalConfiguration config = new XmlFileConfigReader().getConfig(input);

        MutableCollection<MongoDbEnvironment> envs = enricher.readSystem(config, input).getEnvironments();

        validateEnv1(envs.detect(attributeEqual(Environment::getName, "test1")));
        validateEnv2(envs.detect(attributeEqual(Environment::getName, "test2")));
    }

    private void validateEnv1(MongoDbEnvironment env1) {
        assertThat(env1.getConnectionURI(), equalTo("mongodb://localhost:10000"));
        assertThat(env1.getSchemaNames(), equalTo(Sets.immutable.of("MYSCHEMA")));
        assertThat(env1.getPhysicalSchema("MYSCHEMA").getPhysicalName(), equalTo("MYSCHEMA"));
        assertThat(env1.getTokens(), equalTo(Maps.immutable.<String, String>empty()));
    }

    private void validateEnv2(MongoDbEnvironment env2) {
        assertThat(env2.getConnectionURI(), equalTo("mongodb://localhost:10001"));
        assertThat(env2.getSchemaNames(), equalTo(Sets.immutable.of("MYSCHEMA")));
        assertThat(env2.getPhysicalSchema("MYSCHEMA").getPhysicalName(), equalTo("MYSCHEMA_TEST2"));
        assertThat(env2.getTokens(), equalTo(Maps.immutable.of("key", "val")));
    }
}