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

import java.io.IOException;

/**
 * ContentFormatter is responsible for formatting some content related to a Comparison and
 * passing it along to a ContentWriter to actually write.
 * Examples of content include actual breaks, summary data, or the underlying input.
 */
public interface CatoContentFormatter {

    void writeData(CatoComparisonMetadata comparisonMetadata, CatoContentWriter writer) throws IOException;
}
