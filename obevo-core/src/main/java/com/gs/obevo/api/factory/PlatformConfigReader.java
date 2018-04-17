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
package com.gs.obevo.api.factory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.util.vfs.FileObject;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.block.factory.primitive.IntPredicates;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.factory.HashingStrategySets;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to read in properties from default and override locations.
 */
class PlatformConfigReader {
    private static final Logger LOG = LoggerFactory.getLogger(PlatformConfigReader.class);
    private static final String PROP_CONFIG_PRIORITY = "obevo.configPriority";

    public Properties readPlatformProperties(RichIterable<String> configPackages) {
        MutableList<PropertyInput> prioritizedProperties = readConfigPackages(configPackages);

        validate(prioritizedProperties);

        // order properties by priority: higher-numbered files will replace properties of lower-numbered files
        prioritizedProperties.sortThisBy(PropertyInput::getPriority);

        // merge properties
        Properties finalProperties = new Properties();
        for (Properties properties : prioritizedProperties.collect(PropertyInput::getProps)) {
            finalProperties.putAll(properties);
        }

        // remove the configPriority property
        finalProperties.remove(PROP_CONFIG_PRIORITY);

        return finalProperties;
    }

    private MutableList<PropertyInput> readConfigPackages(RichIterable<String> configPackages) {
        MutableSet<PropertyInput> prioritizedProperties = HashingStrategySets.mutable.of(HashingStrategies.fromFunction(PropertyInput::getPropertyFilePath));

        for (String configPackage : configPackages) {
            ListIterable<FileObject> fileObjects = FileRetrievalMode.CLASSPATH.resolveFileObjects(configPackage)
                    .flatCollect(object -> ArrayAdapter.adapt(object.getChildren()));
            ListIterable<FileObject> propertyFiles = fileObjects
                    .select(_this -> _this.getName().getExtension().equals("properties"));

            for (FileObject propertyFile : propertyFiles) {
                Properties fileProps = loadPropertiesFromUrl(propertyFile);

                String configPriorityProp = fileProps.getProperty(PROP_CONFIG_PRIORITY);
                if (configPriorityProp != null) {
                    int priority = Integer.parseInt(configPriorityProp);
                    prioritizedProperties.add(new PropertyInput(propertyFile.getName().getBaseName(), propertyFile.getURLDa(), priority, fileProps));
                } else {
                    LOG.warn("Property file {} was ignored as it did not contain {} property", propertyFile, PROP_CONFIG_PRIORITY);
                }
            }
        }
        return prioritizedProperties.toList();
    }

    private void validate(MutableList<PropertyInput> prioritizedProperties) {
        // Now validate the file inputs
        if (prioritizedProperties.isEmpty()) {
            throw new IllegalStateException("Could not find default configuration " + "abc" + " in the classpath");
        }

        MutableListMultimap<String, PropertyInput> propertiesByFileName = prioritizedProperties.groupBy(PropertyInput::getFileName);
        final MutableList<String> warnings = Lists.mutable.empty();
        final MutableList<String> errors = Lists.mutable.empty();

        propertiesByFileName.forEachKeyMultiValues(new Procedure2<String, Iterable<PropertyInput>>() {
            @Override
            public void value(String fileName, Iterable<PropertyInput> propertyInputsIter) {
                MutableList<PropertyInput> propertyInputs = CollectionAdapter.wrapList(propertyInputsIter);
                MutableBag<Integer> priorities = propertyInputs.collect(PropertyInput::getPriority).toBag();
                MutableBag<Integer> duplicatePriorities = priorities.selectByOccurrences(IntPredicates.greaterThan(1));
                if (duplicatePriorities.notEmpty()) {
                    errors.add("File name [" + fileName + "] was found with the same priority [" + duplicatePriorities + "] in multiple locations [" + propertyInputs.collect(PropertyInput::getPropertyFilePath) + "]. Please ensure that priorities are distinct.");
                } else if (priorities.size() > 1) {
                    warnings.add("File name [" + fileName + "] was found in multiple locations [" + propertyInputs.collect(PropertyInput::getPropertyFilePath) + "]. Will refer to them in their priority order, but ideally the file name should be different.");
                }
            }
        });

        if (warnings.notEmpty()) {
            LOG.warn("Warnings on platform configuration file setup; please address in the future, but program will proceed:\n{}", warnings.makeString("\n"));
        }

        if (errors.notEmpty()) {
            throw new IllegalStateException(errors.makeString("abc"));
        }
    }

    private Properties loadPropertiesFromUrl(FileObject file) {
        Properties props = new Properties();
        InputStream defaultStream = null;
        try {
            defaultStream = file.getURLDa().openStream();
            props.load(defaultStream);
            return props;
        } catch (IOException e) {
            throw new DeployerRuntimeException(e);
        } finally {
            IOUtils.closeQuietly(defaultStream);
        }
    }

    private static class PropertyInput {
        private final String fileName;
        private final URL propertyFilePath;
        private final int priority;
        private final Properties props;

        PropertyInput(String fileName, URL propertyFilePath, int priority, Properties props) {
            this.fileName = fileName;
            this.propertyFilePath = propertyFilePath;
            this.priority = priority;
            this.props = props;
        }

        String getFileName() {
            return fileName;
        }

        URL getPropertyFilePath() {
            return propertyFilePath;
        }

        int getPriority() {
            return priority;
        }

        Properties getProps() {
            return props;
        }
    }
}
