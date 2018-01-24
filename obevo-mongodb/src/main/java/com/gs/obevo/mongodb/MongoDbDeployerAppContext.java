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
package com.gs.obevo.mongodb;

import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeBehavior;
import com.gs.obevo.api.platform.ChangeTypeBehaviorRegistry;
import com.gs.obevo.api.platform.ChangeTypeBehaviorRegistry.ChangeTypeBehaviorRegistryBuilder;
import com.gs.obevo.api.platform.DeployExecutionDao;
import com.gs.obevo.api.platform.FileSourceContext;
import com.gs.obevo.impl.context.AbstractDeployerAppContext;
import com.gs.obevo.impl.PrepareDbChange;
import com.gs.obevo.impl.reader.TableChangeParser.GetChangeType;
import com.mongodb.MongoClient;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.partition.list.PartitionImmutableList;
import org.eclipse.collections.impl.factory.Lists;

public class MongoDbDeployerAppContext extends AbstractDeployerAppContext<MongoDbEnvironment, MongoDbDeployerAppContext> {

    @Override
    protected FileSourceContext getDefaultFileSourceContext() {
        ImmutableList<PrepareDbChange> translators = Lists.immutable.empty();  // need to add translator support
        return new ReaderContext(env, deployStatsTracker(), GetChangeType.DEFAULT_IMPL, null).getDefaultFileSourceContext();
    }

    @Override
    protected ChangeTypeBehaviorRegistryBuilder getChangeTypeBehaviors() {
        ChangeTypeBehaviorRegistryBuilder builder = ChangeTypeBehaviorRegistry.newBuilder();

        PartitionImmutableList<ChangeType> rerunnablePartition = env.getPlatform().getChangeTypes().partition(ChangeType.IS_RERUNNABLE);

        for (ChangeType changeType : rerunnablePartition.getSelected()) {
            builder.put(changeType.getName(), rerunnableSemantic(), deployBehavior());
        }
        for (ChangeType changeType : rerunnablePartition.getRejected()) {
            builder.put(changeType.getName(), incrementalSemantic(), deployBehavior());
        }

        return builder;
    }

    private ChangeTypeBehavior deployBehavior() {
        return singleton("deployBehavior", new Function0<ChangeTypeBehavior>() {
            @Override
            public ChangeTypeBehavior value() {
                return new MongoDeployBehavior(getMongoClient(), env);
            }
        });
    }

    @Override
    protected ChangeAuditDao getArtifactDeployerDao() {
        return new MongoDbChangeAuditDao(getMongoClient(), env, env.getPlatform(), credential.getUsername());
    }

    private MongoClient getMongoClient() {
        return singleton("mongoClient", new Function0<MongoClient>() {
            @Override
            public MongoClient value() {
                return MongoClientFactory.getInstance().getMongoClient(env);
            }
        });
    }

    @Override
    protected DeployExecutionDao getDeployExecutionDao() {
        return new MongoDbDeployExecutionDao(getMongoClient(), env);
    }

    @Override
    public MongoDbDeployerAppContext buildDbContext() {
        return this;
    }

    @Override
    public MongoDbDeployerAppContext buildFileContext() {
        return this;
    }
}
