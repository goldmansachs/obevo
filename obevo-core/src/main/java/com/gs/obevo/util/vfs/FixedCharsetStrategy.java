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
package com.gs.obevo.util.vfs;

import java.nio.charset.Charset;

/**
 * Returns the {@link Charset} that is passed into the constructor, regardless of the bytes involved.
 */
class FixedCharsetStrategy implements CharsetStrategy {
    private final Charset charset;

    public FixedCharsetStrategy(Charset charset) {
        this.charset = charset;
    }

    @Override
    public Charset determineCharset(byte[] bytes) {
        return charset;
    }
}
