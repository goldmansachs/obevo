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
package com.gs.obevo.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class IOUtilsDA {
    public static String toString(InputStream input) {
        try {
            return IOUtils.toString(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toString(URL url) {
        // This method overload was added after 2.0. We have this here for backwards-compatibility with 2.0 for some
        // clients
        // return IOUtils.toString(url);

        try {
            InputStream inputStream = url.openStream();
            try {
                return toString(inputStream);
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
