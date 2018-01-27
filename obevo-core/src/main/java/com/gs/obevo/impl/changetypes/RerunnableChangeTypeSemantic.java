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
package com.gs.obevo.impl.changetypes;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeTypeCommandCalculator;
import com.gs.obevo.impl.graph.GraphEnricher;

/**
 * Implementation for rerunnable changes.
 */
public class RerunnableChangeTypeSemantic extends AbstractChangeTypeSemantic {
    private final GraphEnricher graphEnricher;

    public RerunnableChangeTypeSemantic(GraphEnricher graphEnricher) {
        this.graphEnricher = graphEnricher;
    }

    @Override
    public void manage(Change change, ChangeAuditDao changeAuditDao, DeployExecution deployExecution) {
        changeAuditDao.updateOrInsertChange(change, deployExecution);
    }

    @Override
    public ChangeTypeCommandCalculator getChangeTypeCalculator() {
        return new RerunnableChangeTypeCommandCalculator(graphEnricher);
    }
}
