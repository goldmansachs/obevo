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

import java.text.DecimalFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MemoryMonitor extends Thread {

    private double maxMemory = 0;
    private double currentMemory = 0;
    private boolean live;

    private final long pollFrequency;
    private final boolean logMemoryUsage;

    private final DecimalFormat df = new DecimalFormat("#.##");
    private static final Logger LOG = LoggerFactory.getLogger(MemoryMonitor.class);

    private MemoryMonitor() {
        this(2000, true);
    }

    public MemoryMonitor(long pollFrequency, boolean logMemoryUsage) {
        this.pollFrequency = pollFrequency;
        this.logMemoryUsage = logMemoryUsage;
        this.live = true;
        this.setDaemon(true);
    }

    public void run() {
        while (this.live) {
            try {
                this.currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                this.maxMemory = Math.max(this.currentMemory, this.maxMemory);

                if (this.currentMemory / Runtime.getRuntime().totalMemory() > 0.95) {
                    LOG.warn("JVM memory usage above 95%, using {} of {} available", this.printMemory(this.currentMemory), this.printMemory(Runtime.getRuntime().totalMemory()));
                }

                if (this.logMemoryUsage) {
                    this.logMemoryUsage();
                }

                Thread.sleep(this.pollFrequency);
            } catch (InterruptedException e) {
            }
        }
    }

    private void logMemoryUsage() {
        LOG.info("Currently using {}", this.printMemory(this.currentMemory));
        LOG.info("At peak used {}", this.printMemory(this.maxMemory));
    }

    private String printMemory(double mem) {
        return this.df.format(mem / (1024.0 * 1024.0)) + " MB";
    }

    public double getCurrentMemory() {
        return this.currentMemory;
    }

    public double getMaxMemory() {
        return this.maxMemory;
    }

    public void terminate() {
        this.live = false;
    }
}