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

import java.net.URL;

import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.util.vfs.FileObject;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
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

    public ImmutableHierarchicalConfiguration readPlatformProperties(RichIterable<String> configPackages) {
        MutableList<PropertyInput> prioritizedProperties = readConfigPackages(configPackages);

        validate(prioritizedProperties);

        // order properties by priority: higher-numbered files will replace properties of lower-numbered files
        prioritizedProperties.sortThisBy(new Function<PropertyInput, Integer>() {
            @Override
            public Integer valueOf(PropertyInput propertyInput1) {
                return propertyInput1.getPriority();
            }
        }).reverseThis();  // needs to be reversed as CombinedConfiguration takes the higher-priority files first

        // merge properties
        CombinedConfiguration combinedConfiguration = new CombinedConfiguration(new OverrideCombiner());
        for (HierarchicalConfiguration<ImmutableNode> properties : prioritizedProperties.collect(new Function<PropertyInput, HierarchicalConfiguration<ImmutableNode>>() {
            @Override
            public HierarchicalConfiguration<ImmutableNode> valueOf(PropertyInput propertyInput) {
                return propertyInput.getProps();
            }
        })) {
            combinedConfiguration.addConfiguration(properties);
        }

        // remove the configPriority property
        combinedConfiguration.clearTree(PROP_CONFIG_PRIORITY);

        return combinedConfiguration;
    }

    private MutableList<PropertyInput> readConfigPackages(RichIterable<String> configPackages) {
        MutableSet<PropertyInput> prioritizedProperties = HashingStrategySets.mutable.of(HashingStrategies.fromFunction(new Function<PropertyInput, URL>() {
            @Override
            public URL valueOf(PropertyInput propertyInput) {
                return propertyInput.getPropertyFilePath();
            }
        }));

        for (String configPackage : configPackages) {
            ListIterable<FileObject> fileObjects = FileRetrievalMode.CLASSPATH.resolveFileObjects(configPackage)
                    .flatCollect(new Function<FileObject, Iterable<FileObject>>() {
                        @Override
                        public Iterable<FileObject> valueOf(FileObject object) {
                            return ArrayAdapter.adapt(object.getChildren());
                        }
                    });
            ListIterable<FileObject> propertyFiles = fileObjects
                    .select(new Predicate<FileObject>() {
                        @Override
                        public boolean accept(FileObject it) {
                            return it.getName().getExtension().equals("yaml");
                        }
                    });

            for (FileObject propertyFile : propertyFiles) {
                HierarchicalConfiguration<ImmutableNode> fileProps = loadPropertiesFromUrl(propertyFile);

                String configPriorityProp = fileProps.getString(PROP_CONFIG_PRIORITY);
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

        MutableListMultimap<String, PropertyInput> propertiesByFileName = prioritizedProperties.groupBy(new Function<PropertyInput, String>() {
            @Override
            public String valueOf(PropertyInput propertyInput) {
                return propertyInput.getFileName();
            }
        });
        final MutableList<String> debugMessages = Lists.mutable.empty();
        final MutableList<String> errors = Lists.mutable.empty();

        propertiesByFileName.forEachKeyMultiValues(new Procedure2<String, Iterable<PropertyInput>>() {
            @Override
            public void value(String fileName, Iterable<PropertyInput> propertyInputsIter) {
                MutableList<PropertyInput> propertyInputs = CollectionAdapter.wrapList(propertyInputsIter);
                MutableBag<Integer> priorities = propertyInputs.collect(new Function<PropertyInput, Integer>() {
                    @Override
                    public Integer valueOf(PropertyInput propertyInput1) {
                        return propertyInput1.getPriority();
                    }
                }).toBag();
                MutableBag<Integer> duplicatePriorities = priorities.selectByOccurrences(IntPredicates.greaterThan(1));
                if (duplicatePriorities.notEmpty()) {
                    errors.add("File name [" + fileName + "] was found with the same priority [" + duplicatePriorities + "] in multiple locations [" + propertyInputs.collect(new Function<PropertyInput, URL>() {
                        @Override
                        public URL valueOf(PropertyInput propertyInput) {
                            return propertyInput.getPropertyFilePath();
                        }
                    }) + "]. Please ensure that priorities are distinct.");
                } else if (priorities.size() > 1) {
                    debugMessages.add("File name [" + fileName + "] was found in multiple locations [" + propertyInputs.collect(new Function<PropertyInput, URL>() {
                        @Override
                        public URL valueOf(PropertyInput propertyInput) {
                            return propertyInput.getPropertyFilePath();
                        }
                    }) + "]. Will refer to them in their priority order, but ideally the file name should be different.");
                }
            }
        });

        if (debugMessages.notEmpty()) {
            LOG.debug("Debug notices on platform configuration file setup:\n{}", debugMessages.makeString("\n"));
        }

        if (errors.notEmpty()) {
            throw new IllegalStateException(errors.makeString("abc"));
        }
    }

    private HierarchicalConfiguration<ImmutableNode> loadPropertiesFromUrl(FileObject file) {
        try {
            return new FileBasedConfigurationBuilder<>(YAMLConfiguration.class)
                    .configure(new Parameters().hierarchical().setURL(file.getURLDa()))
                    .getConfiguration();
        } catch (ConfigurationException e) {
            throw new DeployerRuntimeException(e);
        }
    }

    private static class PropertyInput {
        private final String fileName;
        private final URL propertyFilePath;
        private final int priority;
        private final HierarchicalConfiguration<ImmutableNode> props;

        PropertyInput(String fileName, URL propertyFilePath, int priority, HierarchicalConfiguration<ImmutableNode> props) {
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

        HierarchicalConfiguration<ImmutableNode> getProps() {
            return props;
        }
    }
}
