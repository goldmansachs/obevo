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
package com.gs.catodeployany.compare.breaks;

import java.util.Collection;

import com.gs.catodeployany.compare.CatoDataSide;
import com.gs.catodeployany.data.CatoDataObject;

public class GroupBreak extends AbstractBreak {

    final Collection<String> fields;
    final int groupId;

    public GroupBreak(CatoDataObject dataObject, CatoDataSide dataSide, Collection<String> fields, int groupId) {
        super(dataObject, dataSide);
        this.fields = fields;
        this.groupId = groupId;
    }

    public Collection<String> getFields() {
        return this.fields;
    }

    public int getGroupId() {
        return this.groupId;
    }
}
