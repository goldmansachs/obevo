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
package com.gs.obevocomparer.output;

import java.util.List;
import java.util.Set;

import com.gs.obevocomparer.compare.CatoComparison;

/**
 * This inferface contains a lot of metadata about a Comparison.  It is designed to allow
 * ContentFormatters to write data without having to first iterate through the content to identify
 * counts, unique column names, etc.  As data in a Comparison may be stored on disk, each
 * iteration is potentially expensive.
 */
public interface CatoComparisonMetadata {

    CatoComparison getComparison();

    List<BreakTypeInfo> getBreakTypeInfo();

    int getIncludedBreakSize();

    int getExcludedBreakSize();

    boolean hasGroupBreaks();

    Set<String> getLeftFields();

    Set<String> getRightFields();

    Set<String> getFieldBreakFields();

    Set<String> getIncludedFieldBreakFields();

    Set<String> getExcludedFieldBreakFields();

    Set<String> getGroupBreakFields();

    interface BreakTypeInfo extends Comparable<BreakTypeInfo> {

        String getType();

        int getBreakCount();

        int getExcludeCount();
    }
}
