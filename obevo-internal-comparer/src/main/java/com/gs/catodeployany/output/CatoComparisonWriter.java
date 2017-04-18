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
package com.gs.catodeployany.output;

import java.io.IOException;

import com.gs.catodeployany.compare.CatoComparison;

/**
 * This class writes a Comparison object.  Specific implementations are responsible
 * for formatting the Comparison data and writing it to a specific output (excel, text, etc.)
 */
public interface CatoComparisonWriter {

    void writeComparison(CatoComparison comparison) throws IOException;

    void close() throws IOException;
}
