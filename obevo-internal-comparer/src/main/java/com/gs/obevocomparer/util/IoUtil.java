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
package com.gs.obevocomparer.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class IoUtil {

    public static BufferedOutputStream getOutputStream(String fileName) throws IOException {
        return getOutputStream(new File(fileName));
    }

    public static BufferedOutputStream getOutputStream(File file) throws IOException {
        if (file.exists()) {
            file.delete();
        }

        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        return new BufferedOutputStream(new FileOutputStream(file));
    }
}
