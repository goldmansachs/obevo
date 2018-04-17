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
package com.gs.obevo.impl.reader;

import com.gs.obevo.api.appdata.doc.TextMarkupDocument;
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;

import static com.gs.obevo.impl.reader.TextMarkupDocumentReader.TAG_DROP_COMMAND;
import static com.gs.obevo.impl.reader.TextMarkupDocumentReader.TAG_METADATA;
import static com.gs.obevo.impl.reader.TextMarkupDocumentReader.TAG_ROLLBACK;
import static com.gs.obevo.impl.reader.TextMarkupDocumentReader.TAG_ROLLBACK_IF_ALREADY_DEPLOYED;

class TextMarkupDocumentReaderOld {
    private final MutableList<String> firstLevelElements = Lists.mutable.with("CHANGE", TAG_METADATA, TAG_DROP_COMMAND);
    private final MutableList<String> secondLevelElements = Lists.mutable.with(TAG_ROLLBACK,
            TAG_ROLLBACK_IF_ALREADY_DEPLOYED);

    public TextMarkupDocument parseString(String text, TextMarkupDocumentSection otherSection) {
        ImmutableList<TextMarkupDocumentSection> textMarkupDocumentSections = this.parseString(text, this.firstLevelElements, true, "////");

        if (otherSection != null) {
            TextMarkupDocumentSection thisSection = textMarkupDocumentSections.detect(_this -> _this.getName().equals(otherSection.getName()));
            if (thisSection != null) {
                thisSection.mergeAttributes(otherSection);
            } else {
                textMarkupDocumentSections = textMarkupDocumentSections.newWith(otherSection);
            }
        }

        return new TextMarkupDocument(textMarkupDocumentSections);
    }

    private ImmutableList<TextMarkupDocumentSection> parseString(String text, MutableList<String> elementsToCheck, boolean recurse,
            String elementPrefix) {
        MutableList<TextMarkupDocumentSection> sections = Lists.mutable.empty();
        while (true) {
            int earliestIndex = Integer.MAX_VALUE;

            for (String firstLevelElement : elementsToCheck) {
                int index = text.indexOf(elementPrefix + " " + firstLevelElement, 1);
                if (index != -1 && index < earliestIndex) {
                    earliestIndex = index;
                }
            }

            if (earliestIndex == Integer.MAX_VALUE) {
                sections.add(new TextMarkupDocumentSection(null, text));
                break;
            } else {
                sections.add(new TextMarkupDocumentSection(null, text.substring(0, earliestIndex)));
                text = text.substring(earliestIndex);
            }
        }
        for (TextMarkupDocumentSection section : sections) {
            MutableMap<String, String> attrs = Maps.mutable.empty();
            MutableSet<String> toggles = Sets.mutable.empty();
            String content = StringUtils.chomp(section.getContent());

            String[] contents = content.split("\\r?\\n", 2);
            String firstLine = contents[0];

            for (String elementToCheck : elementsToCheck) {
                if (firstLine.startsWith(elementPrefix + " " + elementToCheck)) {
                    section.setName(elementToCheck);
                    String[] args = StringUtils.splitByWholeSeparator(firstLine, " ");
                    for (String arg : args) {
                        if (arg.contains("=")) {
                            String[] attr = arg.split("=");
                            if (attr.length > 2) {
                                throw new IllegalArgumentException("Cannot mark = multiple times in a parameter - "
                                        + firstLine);
                            }
                            String attrVal = attr[1];
                            if (attrVal.startsWith("\"") && attrVal.endsWith("\"")) {
                                attrVal = attrVal.substring(1, attrVal.length() - 1);
                            }
                            attrs.put(attr[0], attrVal);
                        } else {
                            toggles.add(arg);
                        }
                    }
                    if (contents.length > 1) {
                        content = contents[1];
                    } else {
                        content = null;
                    }
                }
            }
            section.setAttrs(attrs.toImmutable());
            section.setToggles(toggles.toImmutable());

            if (!recurse) {
                section.setContent(content);
            } else if (content != null) {
                ImmutableList<TextMarkupDocumentSection> subsections = this.parseString(content, this.secondLevelElements, false, "//");
                if (subsections.size() == 1) {
                    section.setContent(content);
                } else {
                    section.setContent(subsections.get(0).getContent());
                    section.setSubsections(subsections.subList(1, subsections.size()));
                }
            } else {
                section.setContent(null);
            }
        }

        return sections.toImmutable();
    }
}
