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
package com.gs.obevo.util.vfs;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.eclipse.collections.impl.factory.primitive.CharLists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests around reading files, including with encodings.
 *
 * Test writing tip: to create a file w/ a UTF BOM, use Sublime.
 *
 * We have tests with and without BOMs as the original parsing library we used (juniversalchardet) could not handle
 * parsing a string without a BOM. We have since moved to use icu4j, which can detect the encoding even without the BOM
 * character.
 */
public class DetectCharsetStrategyTest {

    @Test
    public void utf8BomTest() {
        FileObject fileObject = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("vfs/FileObject/utf8bom.txt");

        assertEquals(StandardCharsets.UTF_8, fileObject.getDetectedCharset());
        assertEquals("abcdeWithBom", fileObject.getStringContent(CharsetStrategyFactory.getCharsetStrategy(StandardCharsets.UTF_8)));
    }

    @Test
    public void utf16LETest() {
        FileObject fileObject = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("vfs/FileObject/utf16LE.txt");

        assertEquals(StandardCharsets.UTF_16LE, fileObject.getDetectedCharset());
        assertEquals("abcde16LE", fileObject.getStringContent(CharsetStrategyFactory.getCharsetStrategy(StandardCharsets.UTF_16LE)));
    }

    @Test
    public void utf16BETest() {
        FileObject fileObject = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("vfs/FileObject/utf16BE.txt");

        assertEquals(StandardCharsets.UTF_16BE, fileObject.getDetectedCharset());
        assertEquals("abcde16BE", fileObject.getStringContent(CharsetStrategyFactory.getCharsetStrategy(StandardCharsets.UTF_16BE)));
    }

    @Test
    public void utf8NoBomTest() {
        FileObject fileObject = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("vfs/FileObject/utf8regular.txt");

        assertEquals(StandardCharsets.ISO_8859_1, fileObject.getDetectedCharset());
        assertEquals("abcdeNoBom", fileObject.getStringContent(CharsetStrategyFactory.getCharsetStrategy(StandardCharsets.UTF_8)));
    }

    @Test
    public void utf16LENoBomTest() {
        FileObject fileObject = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("vfs/FileObject/utf16LE.nobom.txt");

        assertEquals(StandardCharsets.UTF_16LE, fileObject.getDetectedCharset());
        assertEquals("abcde16LENoBom", fileObject.getStringContent(CharsetStrategyFactory.getCharsetStrategy(StandardCharsets.UTF_16LE)));
    }

    @Test
    public void utf16BENoBomTest() {
        FileObject fileObject = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("vfs/FileObject/utf16BE.nobom.txt");

        assertEquals(StandardCharsets.UTF_16BE, fileObject.getDetectedCharset());
        assertEquals("abcde16BE", fileObject.getStringContent(CharsetStrategyFactory.getCharsetStrategy(StandardCharsets.UTF_16BE)));
    }

    @Test
    public void japaneseCharacterTest() {
        FileObject fileObject = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("vfs/FileObject/unicode-japanese.txt");
        verifyJapaneseStringCharacters(fileObject.getStringContent(CharsetStrategyFactory.getCharsetStrategy(Charset.forName("ISO-2022-JP"))));
        verifyJapaneseStringCharacters(fileObject.getStringContent(CharsetStrategyFactory.getCharsetStrategy(Charset.forName("UTF-16"))), false);
        verifyJapaneseStringCharacters(fileObject.getStringContent(new DetectCharsetStrategy()));
    }

    private void verifyJapaneseStringCharacters(String str) {
        verifyJapaneseStringCharacters(str, true);
    }

    private void verifyJapaneseStringCharacters(String str, boolean pass) {
        char[] expectedChars = { 31109, 12377, 20175, 12395 };

        if (pass) {
            assertEquals(CharLists.mutable.of(expectedChars), CharLists.mutable.of(str.toCharArray()));
        } else {
            assertNotEquals("Invalid test case: expecting a negative/failure use case", CharLists.mutable.of(expectedChars), CharLists.mutable.of(str.toCharArray()));
        }
    }
}
