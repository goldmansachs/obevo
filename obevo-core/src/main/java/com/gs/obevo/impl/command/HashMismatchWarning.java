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
package com.gs.obevo.impl.command;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.impl.ChangeCommandWarning;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

public class HashMismatchWarning implements ChangeCommandWarning {
    private final Change source;
    private final Change deployed;

    public HashMismatchWarning(Change source, Change deployed) {
        this.source = source;
        this.deployed = deployed;
    }

    @Override
    public String getCommandDescription() {
        return "Artifact Hash is mismatching for : " + this.source.getDisplayString() + "; Deployed Version - " + this.deployed.getContentHash() + "; SourceVersion - " + this.source.getContentHash() + "\n\t* Please remeber not to edit already-deployed changes in place, " +
                "and instead add a new Change definition" + this.source.getContentHash();
    }

    @Override
    public boolean isFatal() {
        return true;
    }

    @Override
    public ImmutableList<Change> getChanges() {
        return Lists.immutable.with(this.source);
    }
}
