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
package com.gs.obevo.impl.changetypes;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeTypeCommandCalculator;

/**
 * Implementation for incremental changes.
 */
public class IncrementalChangeTypeSemantic extends AbstractChangeTypeSemantic {
    private final int numThreads;

    public IncrementalChangeTypeSemantic(int numThreads) {
        this.numThreads = numThreads;
    }

    @Override
    public void manage(Change change, ChangeAuditDao changeAuditDao, DeployExecution deployExecution) {
        changeAuditDao.insertNewChange(change, deployExecution);
    }

    @Override
    public ChangeTypeCommandCalculator getChangeTypeCalculator() {
        return new IncrementalChangeTypeCommandCalculator(numThreads);
    }
}
