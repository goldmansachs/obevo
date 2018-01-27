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
package com.gs.obevocomparer.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore("Only a test utility; no tests to run here")
public class TestAppender extends AppenderSkeleton {

    private static final int BUFFER_SIZE = 10000;

    private final ArrayList<LoggingEvent> events = new ArrayList<LoggingEvent>(BUFFER_SIZE);
    private static final Logger LOG = LoggerFactory.getLogger(TestAppender.class);

    @Override
    protected void append(LoggingEvent event) {
        this.events.add(event);

        if (this.events.size() >= BUFFER_SIZE) {
            this.events.clear();
            LOG.info("Clearing LoggingEvents, reached buffer size {}", BUFFER_SIZE);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    public List<LoggingEvent> getEvents() {
        return this.events;
    }
}
