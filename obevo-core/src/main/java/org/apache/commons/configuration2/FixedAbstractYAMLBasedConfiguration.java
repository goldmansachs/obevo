/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.configuration2;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.ConfigurationLogger;
import org.apache.commons.configuration2.tree.ImmutableNode;

/**
 * <p>
 * A base class for configuration implementations based on YAML structures.
 * </p>
 * <p>
 * This base class offers functionality related to YAML-like data structures
 * based on maps. Such a map has strings as keys and arbitrary objects as
 * values. The class offers methods to transform such a map into a hierarchy
 * of {@link ImmutableNode} objects and vice versa.
 * </p>
 *
 * @since 2.2
 */
public class FixedAbstractYAMLBasedConfiguration extends BaseHierarchicalConfiguration
{
    /**
     * Creates a new instance of {@code FixedAbstractYAMLBasedConfiguration}.
     */
    protected FixedAbstractYAMLBasedConfiguration()
    {
        initLogger(new ConfigurationLogger(getClass()));
    }

    /**
     * Creates a new instance of {@code FixedAbstractYAMLBasedConfiguration} as a
     * copy of the specified configuration.
     *
     * @param c the configuration to be copied
     */
    protected FixedAbstractYAMLBasedConfiguration(
            HierarchicalConfiguration<ImmutableNode> c)
    {
        super(c);
        initLogger(new ConfigurationLogger(getClass()));
    }

    /**
     * Loads this configuration from the content of the specified map. The data
     * in the map is transformed into a hierarchy of {@link ImmutableNode}
     * objects.
     *
     * @param map the map to be processed
     */
    protected void load(Map<String, Object> map)
    {
        ImmutableNode.Builder rootBuilder = new ImmutableNode.Builder();
        ImmutableNode top = constructHierarchy(rootBuilder, map);
        getNodeModel().setRootNode(top);
    }

    /**
     * Constructs a YAML map, i.e. String -&gt; Object from a given configuration
     * node.
     *
     * @param node The configuration node to create a map from.
     * @return A Map that contains the configuration node information.
     */
    protected Map<String, Object> constructMap(ImmutableNode node)
    {
        Map<String, Object> map =
                new HashMap<>(node.getChildren().size());
        for (ImmutableNode cNode : node.getChildren())
        {
            if (cNode.getChildren().isEmpty())
            {
                map.put(cNode.getNodeName(), cNode.getValue());
            }
            else
            {
                map.put(cNode.getNodeName(), constructMap(cNode));
            }
        }
        return map;
    }

    /**
     * Constructs the internal configuration nodes hierarchy.
     *
     * @param parent The configuration node that is the root of the current
     *        configuration section.
     * @param map The map with the yaml configurations nodes, i.e. String -&gt;
     *        Object.
     */
    private ImmutableNode constructHierarchy(ImmutableNode.Builder parent,
            Map<String, Object> map)
    {
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map)
            {
                ImmutableNode.Builder subtree =
                        new ImmutableNode.Builder().name(key);
                ImmutableNode children =
                        constructHierarchy(subtree, (Map) value);
                parent.addChild(children);
            }
            // Shant added this fix for the Collection block
            else if (value instanceof Collection)
            {
                boolean areAllChildConfigs = true;
                for (Object element : (Collection) value)
                {
                    if (!(element instanceof Map))
                    {
                        areAllChildConfigs = false;
                        break;
                    }
                }

                if (areAllChildConfigs) {
                    ImmutableNode.Builder subtree =
                            new ImmutableNode.Builder().name(key);

                    for (Object element : (Collection) value) {
                        ImmutableNode children =
                                constructHierarchy(subtree, (Map) element);
                        parent.addChild(children);
                    }
                }
                else
                {
                    ImmutableNode leaf = new ImmutableNode.Builder().name(key)
                            .value(value).create();
                    parent.addChild(leaf);
                }
            }
            else
            {
                ImmutableNode leaf = new ImmutableNode.Builder().name(key)
                        .value(value).create();
                parent.addChild(leaf);
            }
        }
        return parent.create();
    }

    /**
     * Internal helper method to wrap an exception in a
     * {@code ConfigurationException}.
     * @param e the exception to be wrapped
     * @throws ConfigurationException the resulting exception
     */
    static void rethrowException(Exception e) throws ConfigurationException
    {
        if (e instanceof ClassCastException)
        {
            throw new ConfigurationException("Error parsing", e);
        }
        else
        {
            throw new ConfigurationException("Unable to load the configuration",
                    e);
        }
    }
}
