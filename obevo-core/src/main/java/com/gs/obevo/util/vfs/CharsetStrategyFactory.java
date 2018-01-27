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
package com.gs.obevo.util.vfs;

import java.nio.charset.Charset;

/**
 * Returns instances of the {@link CharsetStrategy}.
 */
public class CharsetStrategyFactory {
    public static CharsetStrategy getCharsetStrategy(Charset charset) {
        return new FixedCharsetStrategy(charset);
    }

    /**
     * Returns a charset value based on the input string; meant to be used when reading from property files.
     * "systemdefault" means to use the default system
     * "detect" means to automatically detect
     * Otherwise, it will assume standard values from the Java encodings.
     */
    public static CharsetStrategy getCharsetStrategy(String charsetStrategy) {
        if (charsetStrategy == null || charsetStrategy.equalsIgnoreCase("systemdefault")) {
            return new FixedCharsetStrategy(Charset.defaultCharset());
        } else if (charsetStrategy.equalsIgnoreCase("detect")) {
            return new DetectCharsetStrategy();
        } else {
            return new FixedCharsetStrategy(Charset.forName(charsetStrategy));
        }
    }

    public static CharsetStrategy getDetectCharsetStrategy() {
        return new DetectCharsetStrategy();
    }
}
