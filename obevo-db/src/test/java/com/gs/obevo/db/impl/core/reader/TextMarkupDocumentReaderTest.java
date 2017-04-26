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
package com.gs.obevo.db.impl.core.reader;

import java.util.Arrays;
import java.util.Collection;

import com.gs.obevo.api.appdata.doc.TextMarkupDocument;
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class TextMarkupDocumentReaderTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {true}, {false}
        });
    }

    private final boolean legacyMode;
    private final TextMarkupDocumentReader textMarkupDocumentReader;

    public TextMarkupDocumentReaderTest(boolean legacyMode) {
        this.legacyMode = legacyMode;
        this.textMarkupDocumentReader = new TextMarkupDocumentReader(legacyMode);
    }

    @Test
    public void testSingleSection() {
        TextMarkupDocument doc = textMarkupDocumentReader.parseString(
                "line1\n" +
                        "line2\r\n" +
                        "line3\n"
                , null
        );

        ImmutableList<TextMarkupDocumentSection> sections = doc.getSections();
        assertEquals(1, sections.size());
        this.assertSection(sections.get(0), null, "line1\nline2\r\nline3", UnifiedMap.<String, String>newMap());
    }

    @Test
    public void testRegular() {
        TextMarkupDocument doc = textMarkupDocumentReader.parseString(
                "//// " + TextMarkupDocumentReader.TAG_METADATA + " k1=v1 k2=v2\r\n" +
                        "//// " + TextMarkupDocumentReader.TAG_METADATA + " k3=v3\n" +
                        // "fassdbah\n" + // avoiding this use case
                        "//// CHANGE n=1 a=\"2\" v=3\r\n" +
                        "line1\n" +
                        "line2/*comment to keep */\r\n" +
                        "line3\n" +
                        "//// CHANGE n=2 TOGGLEACTIVE\n" +
                        "//// CHANGE n=3\n" +
                        "finalcontent\r\n" +
                        "// " + TextMarkupDocumentReader.TAG_ROLLBACK_IF_ALREADY_DEPLOYED + " n=3\n" +
                        "rollback content\r\n" +
                        "//// CHANGE n=4\n"
                , null
        );

        verifyRegular(doc);
    }

    @Test
    public void testRegularWithBlankSections() {
        TextMarkupDocument doc = textMarkupDocumentReader.parseString(
                "\n\n\r\n" +
                        "//// " + TextMarkupDocumentReader.TAG_METADATA + " k1=v1 k2=v2\r\n" +
                        "//// " + TextMarkupDocumentReader.TAG_METADATA + " k3=v3\n" +
                        "\n\n\r\n" +
                        "//// CHANGE n=1 a=2 v=3\r\n" +
                        "line1\n" +
                        "line2/*comment to keep */\r\n" +
                        "line3\n" +
                        "//// CHANGE n=2 TOGGLEACTIVE\n" +
                        "//// CHANGE n=3\n" +
                        "finalcontent\r\n" +
                        "// " + TextMarkupDocumentReader.TAG_ROLLBACK_IF_ALREADY_DEPLOYED + " n=3\n" +
                        "rollback content\r\n" +
                        "//// CHANGE n=4\n"
                , null
        );

        verifyRegular(doc);
    }

    @Test
    public void testRegularWithBlankCommentedSections() {
        TextMarkupDocument doc = textMarkupDocumentReader.parseString(
                "\n\n\r\n/*This is my comment that I want to ignore*/\n" +
                        "//// " + TextMarkupDocumentReader.TAG_METADATA + " k1=v1 k2=v2\r\n" +
                        "//// " + TextMarkupDocumentReader.TAG_METADATA + " k3=v3\n" +
                        "\n\n\r\n" +
                        "//// CHANGE n=1 a=2 v=3\r\n" +
                        "line1\n" +
                        "line2/*comment to keep */\r\n" +
                        "line3\n" +
                        "//// CHANGE n=2 TOGGLEACTIVE\n" +
                        "//// CHANGE n=3\n" +
                        "finalcontent\r\n" +
                        "// " + TextMarkupDocumentReader.TAG_ROLLBACK_IF_ALREADY_DEPLOYED + " n=3\n" +
                        "rollback content\r\n" +
                        "//// CHANGE n=4\n"
                , null
        );

        verifyRegular(doc);
    }

    private void verifyRegular(TextMarkupDocument doc) {
        ImmutableList<TextMarkupDocumentSection> sections = doc.getSections();
        assertEquals(6, sections.size());
        this.assertSection(sections.get(0), TextMarkupDocumentReader.TAG_METADATA, null,
                UnifiedMap.<String, String>newWithKeysValues("k1", "v1", "k2", "v2"));
        this.assertSection(sections.get(1), TextMarkupDocumentReader.TAG_METADATA, null,
                UnifiedMap.<String, String>newWithKeysValues("k3", "v3"));
        // assertSection(sections.get(2), null, "blahblahblah\n", UnifiedMap.<String, String>newMap());
        this.assertSection(sections.get(2), TextMarkupDocumentReader.TAG_CHANGE, "line1\nline2/*comment to keep */\r\nline3",
                UnifiedMap.<String, String>newWithKeysValues("n", "1", "a", "2", "v", "3"));
        this.assertSection(sections.get(3), TextMarkupDocumentReader.TAG_CHANGE, null, UnifiedMap.<String, String>newWithKeysValues("n", "2"),
                UnifiedSet.newSetWith("TOGGLEACTIVE"));
        this.assertSection(sections.get(4), TextMarkupDocumentReader.TAG_CHANGE, "finalcontent",
                UnifiedMap.<String, String>newWithKeysValues("n", "3"));
        this.assertSection(sections.get(4).getSubsections().get(0),
                TextMarkupDocumentReader.TAG_ROLLBACK_IF_ALREADY_DEPLOYED, "rollback content",
                UnifiedMap.<String, String>newWithKeysValues("n", "3"));
        this.assertSection(sections.get(5), TextMarkupDocumentReader.TAG_CHANGE, null, UnifiedMap.<String, String>newWithKeysValues("n", "4"));
    }

    @Test
    public void testWithExtraContentAtBeginningAndAfterMetadata() {
        TextMarkupDocument doc = textMarkupDocumentReader.parseString(
                "blahblahblah\n" +
                        "//// " + TextMarkupDocumentReader.TAG_METADATA + " k1=v1 k2=v2\r\n" +
                        "//// " + TextMarkupDocumentReader.TAG_METADATA + " k3=v3\n" +
                        "metadataExtra\n" + // avoiding this use case
                        "//// CHANGE n=1 a=2\n" +
                        "line1\n" +
                        "line2\r\n" +
                        "line3\n" +
                        "//// CHANGE n=2 TOGGLEACTIVE\n" +
                        "//// CHANGE n=3\n" +
                        "finalcontent\r\n" +
                        "// " + TextMarkupDocumentReader.TAG_ROLLBACK_IF_ALREADY_DEPLOYED + " n=3\n" +
                        "rollback content\r\n" +
                        "//// CHANGE n=4\n"
                , null
        );

        ImmutableList<TextMarkupDocumentSection> sections = doc.getSections();
        assertEquals(8, sections.size());
        this.assertSection(sections.get(0), null, "blahblahblah", UnifiedMap.<String, String>newMap());
        this.assertSection(sections.get(1), TextMarkupDocumentReader.TAG_METADATA, null,
                UnifiedMap.<String, String>newWithKeysValues("k1", "v1", "k2", "v2"));
        this.assertSection(sections.get(2), TextMarkupDocumentReader.TAG_METADATA, null,
                UnifiedMap.<String, String>newWithKeysValues("k3", "v3"));
        this.assertSection(sections.get(3), null, "metadataExtra", UnifiedMap.<String, String>newMap());
        this.assertSection(sections.get(4), TextMarkupDocumentReader.TAG_CHANGE, "line1\nline2\r\nline3",
                UnifiedMap.<String, String>newWithKeysValues("n", "1", "a", "2"));
        this.assertSection(sections.get(5), TextMarkupDocumentReader.TAG_CHANGE, null, UnifiedMap.<String, String>newWithKeysValues("n", "2"),
                UnifiedSet.newSetWith("TOGGLEACTIVE"));
        this.assertSection(sections.get(6), TextMarkupDocumentReader.TAG_CHANGE, "finalcontent",
                UnifiedMap.<String, String>newWithKeysValues("n", "3"));
        this.assertSection(sections.get(6).getSubsections().get(0),
                TextMarkupDocumentReader.TAG_ROLLBACK_IF_ALREADY_DEPLOYED, "rollback content",
                UnifiedMap.<String, String>newWithKeysValues("n", "3"));

        this.assertSection(sections.get(7), TextMarkupDocumentReader.TAG_CHANGE, null, UnifiedMap.<String, String>newWithKeysValues("n", "4"));
    }

    private void assertSection(TextMarkupDocumentSection section, String name, String content,
            MutableMap<String, String> attrs) {
        this.assertSection(section, name, content, attrs, UnifiedSet.<String>newSet());
    }

    private void assertSection(final TextMarkupDocumentSection section, String name, String content,
            MutableMap<String, String> attrs, MutableSet<String> toggles) {
        assertEquals(name, section.getName());
        assertEquals(content, section.getContent());
        attrs.forEachKeyValue(new Procedure2<String, String>() {
            @Override
            public void value(String key, String value) {
                assertEquals(value, section.getAttr(key));
            }
        });
        for (String toggle : toggles) {
            assertTrue("Finding toggle " + toggle, section.isTogglePresent(toggle));
        }
    }

    @Test
    public void testSimpleLineParsing() {
        Pair<ImmutableMap<String, String>, ImmutableSet<String>> results = textMarkupDocumentReader.parseAttrsAndToggles(
                "attr=1234 attr2=\"5678\" mytog1"
        );

        assertEquals(Maps.mutable.of("attr", "1234", "attr2", "5678"), results.getOne());
        assertEquals(Sets.mutable.of("mytog1"), results.getTwo());
    }

    @Test
    public void testQuotedSplitWithEqualSign() {
        String input = "attr=1234 attr2=\"56=78\" mytog1";
        if (legacyMode) {
            try {
                textMarkupDocumentReader.parseAttrsAndToggles(input);
                fail("Should have failed here");
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage(), containsString("Cannot mark = multiple times"));
            }
        } else {
            Pair<ImmutableMap<String, String>, ImmutableSet<String>> results = textMarkupDocumentReader.parseAttrsAndToggles(input);

            assertEquals(Maps.mutable.of("attr", "1234", "attr2", "56=78"), results.getOne());
            assertEquals(Sets.mutable.of("mytog1"), results.getTwo());
        }
    }

    @Test
    public void testQuotedSplitWithEqualSignAndSpace() {
        String input = "attr=1234 attr2=\"56 = 78\" mytog1";
        if (legacyMode) {
            try {
                textMarkupDocumentReader.parseAttrsAndToggles(input);
                fail("Should have failed here");
            } catch (ArrayIndexOutOfBoundsException e) {
                assertThat(e.getMessage(), notNullValue());
            }
        } else {
            Pair<ImmutableMap<String, String>, ImmutableSet<String>> results = textMarkupDocumentReader.parseAttrsAndToggles(input);

            assertEquals(Maps.mutable.of("attr", "1234", "attr2", "56 = 78"), results.getOne());
            assertEquals(Sets.mutable.of("mytog1"), results.getTwo());
        }
    }

    @Test
    public void testWordEndingInEqualSignWillFailInLegacy() {
        String input = "   attr= abc";
        if (legacyMode) {
            try {
                textMarkupDocumentReader.parseAttrsAndToggles(input);
                fail("Should have failed here");
            } catch (ArrayIndexOutOfBoundsException expected) {
            }
        } else {
            Pair<ImmutableMap<String, String>, ImmutableSet<String>> results = textMarkupDocumentReader.parseAttrsAndToggles(input);

            assertEquals(Maps.mutable.of("attr", ""), results.getOne());
            assertEquals(Sets.mutable.of("abc"), results.getTwo());
        }
    }

    @Test
    public void testEqualSignInQuoteValueWillFailInLegacy() {
        String input = "   attr=\"abc=123\" ";
        if (legacyMode) {
            try {
                textMarkupDocumentReader.parseAttrsAndToggles(input);
                fail("Should throw exception here");
            } catch (IllegalArgumentException expected) {
            }
        } else {
            Pair<ImmutableMap<String, String>, ImmutableSet<String>> results = textMarkupDocumentReader.parseAttrsAndToggles(input);
            assertEquals(Maps.mutable.of("attr", "abc=123"), results.getOne());
            assertEquals(Sets.mutable.empty(), results.getTwo());
        }
    }

    @Test
    public void testEqualSignAloneWillFailInLegacy() {
        String input = " a = b ";
        if (legacyMode) {
            try {
                textMarkupDocumentReader.parseAttrsAndToggles(input);
                fail("Should throw exception here");
            } catch (ArrayIndexOutOfBoundsException expected) {
            }
        } else {
            Pair<ImmutableMap<String, String>, ImmutableSet<String>> results = textMarkupDocumentReader.parseAttrsAndToggles(input);
            assertEquals(Maps.mutable.empty(), results.getOne());
            assertEquals(Sets.mutable.of("a", "b", "="), results.getTwo());
        }
    }

    @Test
    public void testQuoteWithinQuotedStringIsPreservedIfNoSpace() {
        Pair<ImmutableMap<String, String>, ImmutableSet<String>> results = textMarkupDocumentReader.parseAttrsAndToggles(
                "   abc=attr  1234 attr2=\"56\"78\" mytog1  "
        );

        assertEquals(Maps.mutable.of("abc", "attr", "attr2", "56\"78"), results.getOne());
        assertEquals(Sets.mutable.of("1234", "mytog1"), results.getTwo());
    }

    @Test
    public void testQuoteWithinStringIsStillPreservedEvenIfStringIsntClosedForBackwardsCompatibility() {
        Pair<ImmutableMap<String, String>, ImmutableSet<String>> results = textMarkupDocumentReader.parseAttrsAndToggles(
                "   =attr  1234 attr2=\"56\"78 mytog1  "
        );

        if (legacyMode) {
            assertEquals(Maps.mutable.of("", "attr", "attr2", "\"56\"78"), results.getOne());
            assertEquals(Sets.mutable.of("1234", "mytog1"), results.getTwo());
        } else {
            assertEquals(Maps.mutable.of("attr2", "\"56\"78"), results.getOne());
            assertEquals(Sets.mutable.of("=", "attr", "1234", "mytog1"), results.getTwo());
        }
    }

    @Test
    public void testBehaviorOfStringStartingWithEqualSign() {
        Pair<ImmutableMap<String, String>, ImmutableSet<String>> results = textMarkupDocumentReader.parseAttrsAndToggles(
                " e  q =attr  1234 attr2=\"5678\"\" \"\"mytog1\"  "
        );

        if (legacyMode) {
            assertEquals(Maps.mutable.of("", "attr", "attr2", "5678\""), results.getOne());
            assertEquals(Sets.mutable.of("1234", "\"\"mytog1\"", "e", "q"), results.getTwo());
        } else {
            assertEquals(Maps.mutable.of("attr2", "5678\""), results.getOne());
            assertEquals(Sets.mutable.of("=", "attr", "1234", "\"\"mytog1\"", "e", "q"), results.getTwo());
        }
    }

    @Test
    public void testSlash() {
        Pair<ImmutableMap<String, String>, ImmutableSet<String>> results = textMarkupDocumentReader.parseAttrsAndToggles(
                " 123  attr=\"abc\\def\"  \\  456=789"
        );

        if (legacyMode) {
            assertEquals(Maps.mutable.of("attr", "abc\\def", "456", "789"), results.getOne());
            assertEquals(Sets.mutable.of("123", "\\"), results.getTwo());
        } else {
            assertEquals(Maps.mutable.of("attr", "abc\\def", "456", "789"), results.getOne());
            assertEquals(Sets.mutable.of("123", "\\"), results.getTwo());
        }
    }

    @Test
    public void testSlashWithQuoteNoEscaping2() {
        Pair<ImmutableMap<String, String>, ImmutableSet<String>> results = textMarkupDocumentReader.parseAttrsAndToggles(
                " 123  attr=\"abc\\d ef\"  \\  456=789"
        );

        if (legacyMode) {
            assertEquals(Maps.mutable.of("attr", "\"abc\\d", "456", "789"), results.getOne());
            assertEquals(Sets.mutable.of("123", "\\", "ef\""), results.getTwo());
        } else {
            assertEquals(Maps.mutable.of("attr", "abc\\d ef", "456", "789"), results.getOne());
            assertEquals(Sets.mutable.of("123", "\\"), results.getTwo());
        }
    }

    @Test(expected = RuntimeException.class)  // not allowed in either case
    public void testConsecutiveEquals() {
        textMarkupDocumentReader.parseAttrsAndToggles(
                " 123  attr==val1 b"
        );
    }

    @Test
    public void testSlashWithQuoteNoEscaping() {
        Pair<ImmutableMap<String, String>, ImmutableSet<String>> results = textMarkupDocumentReader.parseAttrsAndToggles(
                " 123  attr=\"abc\\\"d ef\"  \\  456=789"
        );

        if (legacyMode) {
            assertEquals(Maps.mutable.of("attr", "\"abc\\\"d", "456", "789"), results.getOne());
            assertEquals(Sets.mutable.of("123", "\\", "ef\""), results.getTwo());
        } else {
            assertEquals(Maps.mutable.of("attr", "abc\"d ef", "456", "789"), results.getOne());
            assertEquals(Sets.mutable.of("123", "\\"), results.getTwo());
        }
    }

}
