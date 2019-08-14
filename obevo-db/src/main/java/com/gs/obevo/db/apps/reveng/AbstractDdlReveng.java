package com.gs.obevo.db.apps.reveng;

/*
 * Copyright 2017 Goldman Sachs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.gs.obevo.apps.reveng.AbstractReveng;
import com.gs.obevo.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.impl.util.MultiLineStringSplitter;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;

public abstract class AbstractDdlReveng extends AbstractReveng {
    public AbstractDdlReveng(DbPlatform platform, MultiLineStringSplitter stringSplitter, ImmutableList<Predicate<String>> skipPredicates, ImmutableList immutableList, Procedure2 postProcessChange) {
        super(platform, stringSplitter, skipPredicates, immutableList, postProcessChange);
    }

    protected DbEnvironment getDbEnvironment(AquaRevengArgs args) {
        DbPlatform platform = (DbPlatform) this.platform;
        DbEnvironment env = new DbEnvironment();
        env.setPlatform(platform);
        env.setSystemDbPlatform(platform);
        env.setDbHost(args.getDbHost());
        if (args.getDbPort() != null) {
            env.setDbPort(args.getDbPort());
        }
        env.setDbServer(args.getDbServer());
        env.setJdbcUrl(args.getJdbcUrl());
        if (args.getDriverClass() != null) {
            env.setDriverClassName(args.getDriverClass());
        } else {
            env.setDriverClassName(platform.getDriverClass(env).getName());
        }
        return env;
    }
}
