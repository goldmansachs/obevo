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
package com.gs.obevo.impl;

import java.io.Serializable;

import com.gs.obevo.api.platform.DeployMetrics;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.block.factory.Functions0;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

/**
 * Implementation of {@link DeployMetrics}. We separate the implementation from interface to hide the addMetric method.
 */
public class DeployMetricsImpl implements DeployMetrics {
    private final ConcurrentMutableMap<String, Object> statMap = new ConcurrentHashMap<String, Object>();

    void addMetric(String key, Serializable value) {
        statMap.put(key, value);
    }

    void addListMetric(String key, final Serializable value) {
        statMap.updateValue(key, Functions0.newFastList(), listObject -> ((MutableList<Serializable>) listObject).with(value));
    }

    @Override
    public ImmutableMap<String, Object> toSerializedForm() {
        return statMap.toImmutable();
    }
}
