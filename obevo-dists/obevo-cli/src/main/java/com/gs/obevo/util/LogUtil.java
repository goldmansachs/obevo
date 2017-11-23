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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;

public class LogUtil {
    private static final DateTimeFormatter LOG_NAME_TIME_FORMAT = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    public interface FileLogger extends Closeable {
        String getLogFile();
    }

    public static FileLogger getLogAppender(String commandName) {
        // append the user name to the temp directory to 1) avoid conflicts in user folder  2) make it clearer who owned the directory
        String userNameSuffix = SystemUtils.USER_NAME != null ? "-" + SystemUtils.USER_NAME : "";
        File workDir;
        try {
            workDir = Files.createTempDirectory("obevo" + userNameSuffix).toFile();
        } catch (IOException e) {
            // fall back to old logic just in case the above fails (as of v6.1.0)
            workDir = new File(FileUtils.getTempDirectory(), ".obevo" + userNameSuffix);
        }
        final String logFileName = "obevo-" + commandName + "-" + LOG_NAME_TIME_FORMAT.print(new DateTime()) + ".log";

        return LogUtil.configureLogging(workDir, logFileName);
    }

    private static FileLogger configureLogging(File workDir, String fileName) {
        if (!workDir.isDirectory()) {
            workDir.mkdirs();
        }

        return new LogbackFileLogger(workDir + "/" + fileName);
    }

    private static class LogbackFileLogger implements FileLogger {
        private final FileAppender<ILoggingEvent> appender;

        private LogbackFileLogger(String logFilePath) {
            this.appender = createAppender(logFilePath);
            getRootLogger().addAppender(appender);
        }

        @Override
        public String getLogFile() {
            return appender.getFile();
        }

        @Override
        public void close() throws IOException {
            getRootLogger().detachAppender(appender);
        }

        private Logger getRootLogger() {
            return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        }

        private FileAppender<ILoggingEvent> createAppender(String logFilePath) {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            PatternLayoutEncoder ple = new PatternLayoutEncoder();

            ple.setPattern("[%p] %c{1} [%t] %d{[M-dd HH:mm:ss]} - %m%n");
            ple.setContext(lc);
            ple.start();
            FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
            fileAppender.setFile(logFilePath);
            fileAppender.setEncoder(ple);
            fileAppender.setContext(lc);
            fileAppender.start();

            return fileAppender;
        }
    }
}
