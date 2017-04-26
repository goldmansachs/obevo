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
package com.gs.obevocomparer.config;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.fail;

public class CatoResourcesTest {
    static final Logger LOG = LoggerFactory.getLogger(CatoResourcesTest.class);

    private CatoResources catoResources;

    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        this.catoResources = new CatoResources(this.getClass().getClassLoader()
                .getResourceAsStream("TestResources.xml"));
        LOG.info("Cato resources created.");
    }

    /**
     * Test method for
     * {@link com.gs.obevocomparer.config.CatoResources#CatoResources(String)}
     * .
     */
    @Test
    public void testCatoResources() {
        try {
            new CatoResources(this.getClass().getClassLoader()
                    .getResourceAsStream("TestResources.xml"));
            LOG.info("Cato resources created.");
        } catch (CatoResourcesException e) {
            LOG.error("Unable to read the resources - {}", e.getMessage());
            fail("Unable to read the resources - " + e.getMessage());
        }
    }

    /**
     * Test method for
     * {@link com.gs.obevocomparer.config.CatoResources#getDataSource(String)}
     * .
     */
    @Test
    public void testGetDataSource() {
        DataSourceConfig dsc = this.catoResources.getDataSource("ds0");
        if (dsc == null) {
            fail("Could not find data source ds0.");
        }
    }

    /**
     * Test method for
     * {@link com.gs.obevocomparer.config.CatoResources#getRecon(String)}.
     */
    @Test
    public void testGetRecon() {
        ReconConfig rc = this.catoResources.getRecon("recon0");
        if (rc == null) {
            fail("Could not find recon recon0");
        }
    }

    /**
     * Test method for
     * {@link com.gs.obevocomparer.config.CatoResources#addDataSource(com.gs.obevocomparer.config.DataSourceConfig)}
     * .
     */
    @Test
    public void testAddDataSource() {
        DatabaseConfig dbconfig = new DatabaseConfig("ds2", "user2",
                "password2", "url2", "query2");
        this.catoResources.addDataSource(dbconfig);
        if (this.catoResources.getDataSource("ds2") == null) {
            fail("Could not add data source ds2.");
        }
    }

    /**
     * Test method for
     * {@link com.gs.obevocomparer.config.CatoResources#removeDataSource(com.gs.obevocomparer.config.DataSourceConfig)}
     * .
     */
    @Test
    public void testRemoveDataSource() {
        DataSourceConfig dsc = this.catoResources.getDataSource("ds0");
        this.catoResources.removeDataSource(dsc);
        dsc = this.catoResources.getDataSource("ds0");
        if (dsc != null) {
            fail("Could not remove data source ds2.");
        }
    }

    /**
     * Test method for
     * {@link com.gs.obevocomparer.config.CatoResources#addRecon(com.gs.obevocomparer.config.ReconConfig)}
     * .
     */
    @Test
    public void testAddRecon() {
        ReconConfig rc = new ReconConfig();
        rc.setReconName("recon1");
        this.catoResources.addRecon(rc);
        rc = this.catoResources.getRecon("recon1");
        if (rc == null) {
            fail("Unable to add recon recon1.");
        }
    }

    /**
     * Test method for
     * {@link com.gs.obevocomparer.config.CatoResources#removeRecon(com.gs.obevocomparer.config.ReconConfig)}
     * .
     */
    @Test
    public void testRemoveRecon() {
        ReconConfig rc = this.catoResources.getRecon("recon0");
        this.catoResources.removeRecon(rc);
        rc = this.catoResources.getRecon("recon0");
        if (rc != null) {
            fail("Unable to remove recon - recon0");
        }
    }
}
