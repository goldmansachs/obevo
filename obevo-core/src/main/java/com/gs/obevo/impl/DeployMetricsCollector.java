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

/**
 * Convenience component for collecting metrics data from various components for a given execution into a single
 * {@link DeployMetrics} instance. We separate this class and DeployMetrics so that clients can get a
 * read-only view of the metrics.
 *
 * We will eventually replace this w/ the CodaHale metrics.
 */
public interface DeployMetricsCollector {
    /**
     * Register the given metric into the active request. If no active request, throws an exception.
     */
    void addMetric(String key, Serializable value);

    /**
     * Register the given metric into the active request. The metric will be stored as a list, with the input value
     * added to the list. If no active request, throws an exception.
     */
    void addListMetric(String key, Serializable value);

    /**
     * Ends the request and returns the metrics gathered during the execution.
     */
    DeployMetrics getMetrics();
}
