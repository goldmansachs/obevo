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
import com.gs.obevo.api.appdata.GroupChange;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeTypeCommandCalculator;
import com.gs.obevo.impl.changecalc.ChangeCommandFactory;
import com.gs.obevo.impl.graph.GraphEnricher;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * Semantics around rerunnable {@link GroupChange} changes.
 */
public class GroupChangeTypeSemantic extends AbstractChangeTypeSemantic {
    private final GraphEnricher graphEnricher;

    public GroupChangeTypeSemantic(GraphEnricher graphEnricher) {
        this.graphEnricher = graphEnricher;
    }

    public static ImmutableList<Change> getSubChanges(Change change) {
        return ((GroupChange) change).getChanges();
    }

    @Override
    public void manage(Change change, ChangeAuditDao changeAuditDao, DeployExecution deployExecution) {
        for (Change staticData : getSubChanges(change)) {
            changeAuditDao.updateOrInsertChange(staticData, deployExecution);
        }
    }

    @Override
    public ChangeTypeCommandCalculator getChangeTypeCalculator() {
        return new GroupChangeTypeCommandCalculator(new ChangeCommandFactory(), graphEnricher);
    }
}
