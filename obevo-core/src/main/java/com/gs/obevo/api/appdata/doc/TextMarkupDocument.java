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

import java.util.Objects;

import org.eclipse.collections.api.list.ImmutableList;

public class TextMarkupDocument {
    private final ImmutableList<TextMarkupDocumentSection> sections;

    public TextMarkupDocument(ImmutableList<TextMarkupDocumentSection> sections) {
        this.sections = sections;
    }

    public ImmutableList<TextMarkupDocumentSection> getSections() {
        return this.sections;
    }

    public TextMarkupDocumentSection findSectionWithElementName(String elementName) {
        return this.sections.detect(_this -> Objects.equals(elementName, _this.getName()));
    }
}
