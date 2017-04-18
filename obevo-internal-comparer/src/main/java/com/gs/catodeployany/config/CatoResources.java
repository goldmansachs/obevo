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
package com.gs.catodeployany.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class CatoResources {
    static final Logger LOG = LoggerFactory.getLogger(CatoResources.class);

    private Map<String, DataSourceConfig> dataSources;

    private Map<String, ReconConfig> recons;

    private String resourcesFilePath;

    public CatoResources(InputStream is) throws CatoResourcesException {
        this.initialize(is);
    }

    public CatoResources(String resfile) throws CatoResourcesException {
        this(new File(resfile));
        this.resourcesFilePath = resfile;
    }

    public CatoResources(File resFile) throws CatoResourcesException {
        File resourcesFile = resFile;
        try {
            FileInputStream fis = new FileInputStream(resourcesFile);
            this.initialize(fis);
            fis.close();
        } catch (FileNotFoundException e) {
            LOG.warn("Unable to read the resources xml - {}. Will continue with empty config and create one on exit.", resourcesFile.getName());
            this.dataSources = new HashMap<String, DataSourceConfig>();
            this.recons = new HashMap<String, ReconConfig>();
        } catch (IOException e) {
            throw new CatoResourcesException(
                    "Unable to read the resources xml - "
                            + resourcesFile.getName(), e);
        }
    }

    private void initialize(InputStream xmlInputStream)
            throws CatoResourcesException {
        this.dataSources = new HashMap<String, DataSourceConfig>();
        this.recons = new HashMap<String, ReconConfig>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document resourcesDocument = db.parse(xmlInputStream);
            Element resourcesElem = resourcesDocument.getDocumentElement();
            NodeList dssElems = resourcesElem
                    .getElementsByTagName("DataSources");
            if (dssElems.getLength() > 0 && dssElems.item(0) != null) {
                Element dssElem = (Element) dssElems.item(0);
                NodeList dsElems = dssElem.getElementsByTagName("DBDataSource");
                for (int i = 0; i < dsElems.getLength(); i++) {
                    Element dsElem = (Element) dsElems.item(i);
                    DatabaseConfig dbconfig = new DatabaseConfig();
                    dbconfig.setName(dsElem.getAttribute("name"));
                    dbconfig.setSorted(Boolean.parseBoolean(dsElem.getAttribute("sorted")));
                    dbconfig.setUser(this.getChildElementValue(dsElem, "user"));
                    dbconfig.setPassword(this.getChildElementValue(dsElem,
                            "password"));
                    dbconfig.setQuery(this.getChildElementValue(dsElem, "query"));
                    dbconfig.setURL(this.getChildElementValue(dsElem, "url"));
                    this.dataSources.put(dbconfig.getName(), dbconfig);
                    LOG.info("Added new data source - {}", dbconfig);
                }
                NodeList tdsElems = dssElem
                        .getElementsByTagName("TextDataSource");
                for (int i = 0; i < tdsElems.getLength(); i++) {
                    Element tdsElem = (Element) tdsElems.item(i);
                    TextConfig txtconfig = new TextConfig();
                    txtconfig.setName(tdsElem.getAttribute("name"));
                    txtconfig.setSorted(Boolean.parseBoolean(tdsElem.getAttribute("sorted")));
                    txtconfig.setFile(this.getChildElementValue(tdsElem, "File"));
                    txtconfig.setDelimiter(this.getChildElementValue(tdsElem,
                            "Delimiter"));
                    this.dataSources.put(txtconfig.getName(), txtconfig);
                    LOG.info("Added new data source - {}", txtconfig);
                }
            }
            NodeList reconsElems = resourcesElem.getElementsByTagName("Recons");
            if (reconsElems.getLength() > 0 && reconsElems.item(0) != null) {
                Element reconsElem = (Element) reconsElems.item(0);
                NodeList reconElems = reconsElem.getElementsByTagName("Recon");
                for (int i = 0; i < reconElems.getLength(); i++) {
                    Element reconElem = (Element) reconElems.item(i);
                    ReconConfig rc = new ReconConfig();
                    rc.setReconName(reconElem.getAttribute("name"));
                    rc.setDataSource1(this.getDataSource(this.getChildElementValue(
                            reconElem, "DataSource1")));
                    rc.setDataSource2(this.getDataSource(this.getChildElementValue(
                            reconElem, "DataSource2")));
                    NodeList fieldElems = reconElem
                            .getElementsByTagName("Field");
                    for (int j = 0; j < fieldElems.getLength(); j++) {
                        Element fieldElem = (Element) fieldElems.item(j);
                        ReconFieldConfig rfc = new ReconFieldConfig(
                                this.getChildElementValue(fieldElem, "Name"));
                        rfc.setAttribute(Boolean.parseBoolean(fieldElem
                                .getAttribute("isAttribute")));
                        rfc.setKey(Boolean.parseBoolean(fieldElem
                                .getAttribute("isKey")));
                        rfc.setExcluded(Boolean.parseBoolean(fieldElem
                                .getAttribute("isExcluded")));
                        rc.addField(rfc);
                    }
                    this.recons.put(rc.getReconName(), rc);
                    LOG.info("Added new recon - {}", rc);
                }
            }
        } catch (ParserConfigurationException e) {
            throw new CatoResourcesException(
                    "Unable to read the resources xml - ", e);
        } catch (SAXException e) {
            throw new CatoResourcesException(
                    "Unable to read the resources xml - ", e);
        } catch (IOException e) {
            throw new CatoResourcesException(
                    "Unable to read the resources xml - ", e);
        }
    }

    private String getChildElementValue(Element elem, String childElemName) {
        NodeList nameNodes = elem.getElementsByTagName(childElemName);
        if (nameNodes.getLength() > 0 && nameNodes.item(0) != null) {
            Element nameElem = (Element) nameNodes.item(0);
            if (nameElem.getFirstChild() != null) {
                return nameElem.getFirstChild().getNodeValue().trim();
            }
        }
        return null;
    }

    public DataSourceConfig getDataSource(String name) {
        for (DataSourceConfig dsc : this.dataSources.values()) {
            if (dsc.getName().equals(name)) {
                return dsc;
            }
        }
        return null;
    }

    public ReconConfig getRecon(String name) {
        for (ReconConfig rc : this.recons.values()) {
            if (rc.getReconName().equals(name)) {
                return rc;
            }
        }
        return null;
    }

    public void addDataSource(DataSourceConfig ds) {
        this.dataSources.put(ds.getName(), ds);
    }

    public DataSourceConfig removeDataSource(DataSourceConfig ds) {
        return this.dataSources.remove(ds.getName());
    }

    public void addRecon(ReconConfig r) {
        this.recons.put(r.getReconName(), r);
    }

    public ReconConfig removeRecon(ReconConfig r) {
        return this.recons.remove(r.getReconName());
    }

    public Collection<DataSourceConfig> getDataSources() {
        return this.dataSources.values();
    }

    public Collection<ReconConfig> getRecons() {
        return this.recons.values();
    }

    public void saveResources() throws CatoResourcesException {
        this.saveResources(this.resourcesFilePath);
    }

    public void saveResources(String saveLocation) throws CatoResourcesException {
        if (this.resourcesFilePath == null) {
            LOG.error("Save is unavailable as the resources file path is null/empty.");
            throw new CatoResourcesException(
                    "Save is unavailable as the resources file path is null/empty.");
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;
            db = dbf.newDocumentBuilder();
            Document resourcesDocument = db.newDocument();
            Element resElem = resourcesDocument.createElement("CatoResources");
            Element reconsElem = resourcesDocument.createElement("Recons");
            Element dataSourcesElem = resourcesDocument
                    .createElement("DataSources");
            resElem.appendChild(reconsElem);
            resElem.appendChild(dataSourcesElem);
            resourcesDocument.appendChild(resElem);

            this.writeReconsToElement(reconsElem);
            this.writeDataSourcesToElement(dataSourcesElem);

            this.writeDocument(resourcesDocument, saveLocation);
        } catch (ParserConfigurationException e) {
            throw new CatoResourcesException(
                    "Uable to create a new resources document.", e);
        }
    }

    /**
     * Write the xml document in pretty printed format on disk
     *
     * @param resourcesDocument the document object to write
     * @throws CatoUIException if there was an error writing the document.
     */
    private void writeDocument(Document resourcesDocument, String saveLocation)
            throws CatoResourcesException {
        File outFile = new File(saveLocation);
        TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new CatoResourcesException(
                    "Unable to write xml output to file. Transformer error.", e);
        }
        DOMSource source = new DOMSource(resourcesDocument);
        StreamResult result = new StreamResult(outFile);
        LOG.info("XML output is written to file - {}", saveLocation);
        try {
            // TODO the pretty printed format does not have correct indenting,
            // use a better pretty printing mechanism
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new CatoResourcesException(
                    "Unable to write xml output to file. Transformer error.", e);
        }
    }

    /**
     * Add a child element to the given element with the given name as element
     * name, and the given text value as value of text node.
     *
     * @return child element
     */
    private Element addChildElement(Element parent, String childElementName,
            String childElementValue) {
        Element child = parent.getOwnerDocument().createElement(
                childElementName);
        this.addText(child, childElementValue);
        parent.appendChild(child);
        return child;
    }

    /**
     * Add a text node with given value to the element.
     */
    private void addText(Element elem, String text) {
        Text textNode = elem.getOwnerDocument().createTextNode(text);
        elem.appendChild(textNode);
    }

    private void writeReconsToElement(Element reconsElem) {
        for (ReconConfig rc : this.recons.values()) {
            Element reconElem = reconsElem.getOwnerDocument().createElement(
                    "Recon");
            reconElem.setAttribute("id", rc.getReconName());
            reconElem.setAttribute("name", rc.getReconName());
            this.addChildElement(reconElem, "DataSource1", rc.getDataSource1()
                    .getName());
            this.addChildElement(reconElem, "DataSource2", rc.getDataSource2()
                    .getName());

            for (ReconFieldConfig rfc : rc.getAllFields()) {
                Element fieldElem = reconsElem.getOwnerDocument()
                        .createElement("Field");
                fieldElem.setAttribute("isKey", String.valueOf(rfc.isKey()));
                fieldElem.setAttribute("isAttribute", String.valueOf(rfc.isAttribute()));
                fieldElem.setAttribute("isExcluded", String.valueOf(rfc.isExcluded()));
                this.addChildElement(fieldElem, "Name", rfc.getName());
                reconElem.appendChild(fieldElem);
            }

            reconsElem.appendChild(reconElem);
        }
    }

    private void writeDataSourcesToElement(Element dataSourcesElem) {
        for (DataSourceConfig dsc : this.dataSources.values()) {
            if (dsc instanceof DatabaseConfig) {
                DatabaseConfig dbdsc = (DatabaseConfig) dsc;
                Element dbElem = dataSourcesElem.getOwnerDocument()
                        .createElement("DBDataSource");
                dbElem.setAttribute("id", dbdsc.getName());
                dbElem.setAttribute("name", dbdsc.getName());
                dbElem.setAttribute("sorted", dbdsc.getSorted().toString());
                this.addChildElement(dbElem, "url", dbdsc.getURL());
                this.addChildElement(dbElem, "query", dbdsc.getQuery());
                this.addChildElement(dbElem, "user", dbdsc.getUser());
                this.addChildElement(dbElem, "password", dbdsc.getPassword());
                dataSourcesElem.appendChild(dbElem);
            } else if (dsc instanceof TextConfig) {
                TextConfig tc = (TextConfig) dsc;
                Element tcElem = dataSourcesElem.getOwnerDocument()
                        .createElement("TextDataSource");
                tcElem.setAttribute("id", tc.getName());
                tcElem.setAttribute("name", tc.getName());
                tcElem.setAttribute("sorted", tc.getSorted().toString());
                this.addChildElement(tcElem, "File", tc.getFile());
                this.addChildElement(tcElem, "Delimiter", tc.getDelimiter());
                dataSourcesElem.appendChild(tcElem);
            }
        }
    }
}
