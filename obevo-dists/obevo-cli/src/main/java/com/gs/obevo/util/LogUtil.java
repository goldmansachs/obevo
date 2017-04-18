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

import java.io.File;
import java.io.IOException;

import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;

public class LogUtil {

    public static FileAppender configureLogging(File workDir, String fileName) {
        if (!workDir.isDirectory()) {
            workDir.mkdirs();
        }

        return configureLogging(workDir + "/" + fileName);
    }

    private static FileAppender configureLogging(String logFilePath) {
        try {
            FileAppender fileAppender = new FileAppender(
                    new PatternLayout("[%p] %c{1} [%t] %d{[M-dd HH:mm:ss]} - %m%n"),
                    logFilePath,
                    false
            );
            org.apache.log4j.Logger.getRootLogger().addAppender(fileAppender);
            return fileAppender;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void closeAppender(FileAppender appender) {
        org.apache.log4j.Logger.getRootLogger().removeAppender(appender);
        appender.close();
    }
}
