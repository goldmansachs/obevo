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
package com.gs.obevo.api.appdata.doc;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;

public class TextMarkupDocumentSection {
    private String name;
    private String content;
    private ImmutableMap<String, String> attrs;
    private ImmutableSet<String> toggles;
    private ImmutableList<TextMarkupDocumentSection> subsections = Lists.immutable.empty();

    public TextMarkupDocumentSection(String name, String content) {
        this(name, content, null);
    }

    public TextMarkupDocumentSection(String name, String content, ImmutableMap<String, String> attrs) {
        this.name = name;
        this.content = content;
        this.toggles = Sets.immutable.empty();
        this.setAttrs(attrs);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAttr(String attrName) {
        return this.attrs.get(attrName);
    }

    public ImmutableMap<String, String> getAttrs() {
        return attrs;
    }

    public void setAttrs(ImmutableMap<String, String> attrs) {
        this.attrs = attrs != null ? attrs : Maps.immutable.<String, String>empty();
    }

    public ImmutableList<TextMarkupDocumentSection> getSubsections() {
        return this.subsections;
    }

    public void setSubsections(
            ImmutableList<TextMarkupDocumentSection> subsections) {
        this.subsections = subsections;
    }

    public boolean isTogglePresent(String toggle) {
        return this.toggles.contains(toggle);
    }

    public ImmutableSet<String> getToggles() {
        return toggles;
    }

    public void setToggles(ImmutableSet<String> toggles) {
        this.toggles = toggles;
    }

    public void mergeAttributes(TextMarkupDocumentSection other) {
        if (other == null) {
            return;
        }
        this.attrs = this.attrs.toMap()
                .withAllKeyValues(other.attrs.select(new Predicate2<String, String>() {
                    @Override
                    public boolean accept(String key, String value) {
                        return !TextMarkupDocumentSection.this.attrs.contains(key);
                    }
                }).keyValuesView())
                .toImmutable();

        this.toggles = this.toggles.newWithAll(other.toggles.select(new Predicate<String>() {
            @Override
            public boolean accept(String key) {
                return !TextMarkupDocumentSection.this.toggles.contains(key);
            }
        }));
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
