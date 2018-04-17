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

import java.util.List;

import com.gs.obevo.api.appdata.doc.TextMarkupDocument;
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import com.gs.obevo.db.sqlparser.textmarkup.TextMarkupLineSyntaxParserConstants;
import com.gs.obevo.db.sqlparser.textmarkup.TextMarkupParser;
import com.gs.obevo.db.sqlparser.textmarkup.Token;
import com.gs.obevo.impl.text.CommentRemover;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;

public class TextMarkupDocumentReader {
    public static final String TAG_CHANGE = "CHANGE";
    public static final String TAG_METADATA = "METADATA";
    public static final String TAG_BODY = "BODY";
    public static final String TAG_DROP_COMMAND = "DROP_COMMAND";
    public static final String TAG_ROLLBACK = "ROLLBACK";  // not yet used elsewhere, unlike the
    // rollbackIfAlreadyDeployed
    public static final String TAG_ROLLBACK_IF_ALREADY_DEPLOYED = "ROLLBACK-IF-ALREADY-DEPLOYED";
    public static final String TOGGLE_DISABLE_QUOTED_IDENTIFIERS = "DISABLE_QUOTED_IDENTIFIERS";
    public static final String ATTR_UPDATE_TIME_COLUMN = "updateTimeColumn";
    public static final String ATTR_DEPENDENCIES = "dependencies";
    public static final String ATTR_EXCLUDE_DEPENDENCIES = "excludeDependencies";
    public static final String ATTR_INCLUDE_DEPENDENCIES = "includeDependencies";
    public static final String ATTR_PRIMARY_KEYS = "primaryKeys";
    public static final String INCLUDE_ENVS = "includeEnvs";
    public static final String EXCLUDE_ENVS = "excludeEnvs";
    public static final String INCLUDE_PLATFORMS = "includePlatforms";
    public static final String EXCLUDE_PLATFORMS = "excludePlatforms";
    public static final String TAG_PERM_SCHEME = "permissionScheme";

    private final ImmutableList<String> firstLevelElements = Lists.immutable.with(TAG_CHANGE, TAG_DROP_COMMAND, TAG_METADATA, TAG_BODY);
    private final ImmutableList<String> singleLineElements = Lists.immutable.with(TAG_METADATA);
    private final ImmutableList<String> secondLevelElements = Lists.immutable.with(TAG_ROLLBACK_IF_ALREADY_DEPLOYED, TAG_ROLLBACK);

    private final boolean legacyMode;

    public TextMarkupDocumentReader(boolean legacyMode) {
        this.legacyMode = legacyMode;
    }

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

    private ImmutableList<TextMarkupDocumentSection> parseString(String text, ImmutableList<String> elementsToCheck, final boolean recurse,
            final String elementPrefix) {
        MutableList<Pair<String, String>> outerSections = splitIntoMainSections(text, elementsToCheck, elementPrefix);

        MutableList<TextMarkupDocumentSection> sections = outerSections.flatCollect(new ConvertOuterSectionToTextSection(recurse, elementPrefix));

        // remove any blank sections
        return sections.toImmutable().reject(new Predicate<TextMarkupDocumentSection>() {
            @Override
            public boolean accept(TextMarkupDocumentSection each) {
                return recurse && each.getName() == null
                        && (StringUtils.isBlank(each.getContent())
                        || StringUtils.isBlank(CommentRemover.removeComments(each.getContent(), "removing on markup document reader"))  // need comments in a separate clause as CommentRemover returns a "null" string on null; will fix eventually
                );
            }
        });
    }

    private MutableList<Pair<String, String>> splitIntoMainSections(String text, ImmutableList<String> elementsToCheck, String elementPrefix) {
        MutableList<Pair<String, String>> outerSections = Lists.mutable.empty();
        String nextSectionName = null;
        boolean startOfSearch = true;

        // here, we go in a loop searching for the next referenc of "[elementPrefix] [sectionName]", e.g. //// CHANGE
        // By each of those points, we split those into separate text sections and return back to the client.
        // We aim to preserve the line breaks found when parsing the sections
        while (text != null) {
            String currentSectionName = nextSectionName;
            String currentSectionText;

            int earliestIndex = Integer.MAX_VALUE;

            for (String firstLevelElement : elementsToCheck) {
                // on the first search, the text may start w/ the section; hence, we set the search fromIndex param to 0.
                // Subsequently, the index picks up at the beginning of the next section; hence, we must start
                // the search at the next character, so the fromIndex param is 1
                int index = text.indexOf(elementPrefix + " " + firstLevelElement, startOfSearch ? 0 : 1);

                if (index != -1 && index < earliestIndex) {
                    earliestIndex = index;
                    nextSectionName = firstLevelElement;
                }
            }

            startOfSearch = false;

            if (earliestIndex == Integer.MAX_VALUE) {
                currentSectionText = StringUtils.chomp(text);
                text = null;
            } else {
                currentSectionText = StringUtils.chomp(text.substring(0, earliestIndex));
                text = text.substring(earliestIndex);
            }

            outerSections.add(Tuples.pair(currentSectionName, currentSectionText));
        }
        return outerSections;
    }

    private class ConvertOuterSectionToTextSection implements Function<Pair<String, String>, Iterable<TextMarkupDocumentSection>> {
        private final boolean recurse;
        private final String elementPrefix;

        ConvertOuterSectionToTextSection(boolean recurse, String elementPrefix) {
            this.elementPrefix = elementPrefix;
            this.recurse = recurse;
        }

        @Override
        public Iterable<TextMarkupDocumentSection> valueOf(Pair<String, String> outerSection) {
            String currentSectionName = outerSection.getOne();
            if (currentSectionName == null) {
                return Lists.mutable.with(new TextMarkupDocumentSection(null, outerSection.getTwo()));
            } else {
                String[] contents = outerSection.getTwo().split("\\r?\\n", 2);
                String firstLine = contents[0];
                firstLine = firstLine.replaceFirst(elementPrefix + " " + currentSectionName, "");

                Pair<ImmutableMap<String, String>, ImmutableSet<String>> attrsTogglesPair = parseAttrsAndToggles(firstLine);
                ImmutableMap<String, String> attrs = attrsTogglesPair.getOne();
                ImmutableSet<String> toggles = attrsTogglesPair.getTwo();

                String sectionContent = contents.length > 1 ? contents[1] : null;
                if (singleLineElements.contains(currentSectionName)) {
                    TextMarkupDocumentSection metadataSection = new TextMarkupDocumentSection(currentSectionName, null, attrs.toImmutable());
                    metadataSection.setToggles(toggles.toImmutable());
                    return Lists.mutable.with(metadataSection, new TextMarkupDocumentSection(null, sectionContent));
                } else {
                    ImmutableList<TextMarkupDocumentSection> finalsubsections = Lists.immutable.empty();
                    String finalContent;
                    if (!recurse) {
                        finalContent = sectionContent;
                    } else if (sectionContent != null) {
                        ImmutableList<TextMarkupDocumentSection> subsections = parseString(sectionContent, secondLevelElements, false, "//");
                        if (subsections.size() == 1) {
                            finalContent = sectionContent;
                        } else {
                            finalContent = subsections.get(0).getContent();
                            finalsubsections = subsections.subList(1, subsections.size());
                        }
                    } else {
                        finalContent = null;
                    }

                    TextMarkupDocumentSection section = new TextMarkupDocumentSection(currentSectionName, finalContent, attrs.toImmutable());
                    section.setToggles(toggles.toImmutable());
                    section.setSubsections(finalsubsections);
                    return Lists.mutable.with(section);
                }
            }
        }
    }

    Pair<ImmutableMap<String, String>, ImmutableSet<String>> parseAttrsAndToggles(String line) {
        MutableMap<String, String> attrs = Maps.mutable.empty();
        MutableSet<String> toggles = Sets.mutable.empty();

        if (!legacyMode) {
            List<Token> tokens = TextMarkupParser.parseTokens(line);
            Token curToken = !tokens.isEmpty() ? tokens.get(0) : null;
            while (curToken != null && curToken.kind != TextMarkupLineSyntaxParserConstants.EOF) {
                switch (curToken.kind) {
                case TextMarkupLineSyntaxParserConstants.WHITESPACE:
                    // skip whitespace if encountered
                    break;
                case TextMarkupLineSyntaxParserConstants.QUOTED_LITERAL:
                case TextMarkupLineSyntaxParserConstants.STRING_LITERAL:
                    // let's check if this is a toggle or an attribute
                    if (curToken.next.kind == TextMarkupLineSyntaxParserConstants.ASSIGN) {
                        Token keyToken = curToken;
                        curToken = curToken.next;  // to ASSIGN
                        curToken = curToken.next;  // to the following token
                        switch (curToken.kind) {
                        case TextMarkupLineSyntaxParserConstants.QUOTED_LITERAL:
                        case TextMarkupLineSyntaxParserConstants.STRING_LITERAL:
                            // in this case, we have an attribute value
                            String value = curToken.image;
                            if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                                value = curToken.image.substring(1, curToken.image.length() - 1);
                            }
                            value = value.replaceAll("\\\\\"", "\"");
                            attrs.put(keyToken.image, value);
                            break;
                        case TextMarkupLineSyntaxParserConstants.WHITESPACE:
                        case TextMarkupLineSyntaxParserConstants.EOF:
                            // in this case, we will assume a blank value
                            attrs.put(keyToken.image, "");
                            break;
                        case TextMarkupLineSyntaxParserConstants.ASSIGN:
                        default:
                            throw new IllegalStateException("Not allowed here");
                        }
                    } else {
                        toggles.add(curToken.image);
                    }
                    break;
                case TextMarkupLineSyntaxParserConstants.ASSIGN:
                    toggles.add(curToken.image);
                    break;
                case TextMarkupLineSyntaxParserConstants.EOF:
                default:
                    throw new IllegalStateException("Should not arise");
                }

                curToken = curToken.next;
            }
        } else {
            // keeping this mode for backwards-compatibility until we can guarantee all clients are fine without it
            // This way cannot handle spaces in quotes
            String[] args = StringUtils.splitByWholeSeparator(line, " ");

            for (String arg : args) {
                if (arg.contains("=")) {
                    String[] attr = arg.split("=");
                    if (attr.length > 2) {
                        throw new IllegalArgumentException("Cannot mark = multiple times in a parameter - " + line);
                    }
                    String attrVal = attr[1];
                    if (attrVal.startsWith("\"") && attrVal.endsWith("\"")) {
                        attrVal = attrVal.substring(1, attrVal.length() - 1);
                    }
                    attrs.put(attr[0], attrVal);
                } else if (StringUtils.isNotBlank(arg)) {
                    toggles.add(arg);
                }
            }
        }

        return Tuples.pair(attrs.toImmutable(), toggles.toImmutable());
    }
}
