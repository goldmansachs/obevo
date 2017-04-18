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
package com.gs.catodeployany.util;

import org.junit.Assert;
import org.junit.Test;

public class MemoryMonitorTest {

    @Test
    public void testMemoryMonitor() {
        MemoryMonitor monitor = new MemoryMonitor(10, true);

        Assert.assertEquals(0.0, monitor.getCurrentMemory(), 0.01);
        Assert.assertEquals(0.0, monitor.getMaxMemory(), 0.01);
        Assert.assertTrue(monitor.isDaemon());

        monitor.start();
        this.sleep(100);

        Assert.assertTrue(monitor.isAlive());
        Assert.assertTrue(monitor.getCurrentMemory() > 0);
        Assert.assertTrue(monitor.getMaxMemory() > 0);

        monitor.terminate();
        this.sleep(50);

        Assert.assertFalse(monitor.isAlive());
        org.junit.Assert.assertTrue(monitor.getCurrentMemory() > 0);
        org.junit.Assert.assertTrue(monitor.getMaxMemory() > 0);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
        }
    }
}
