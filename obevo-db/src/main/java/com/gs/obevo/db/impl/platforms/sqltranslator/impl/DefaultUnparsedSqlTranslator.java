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
package com.gs.obevo.db.impl.platforms.sqltranslator.impl;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gs.obevo.api.appdata.ChangeInput;
import com.gs.obevo.db.impl.platforms.sqltranslator.UnparsedSqlTranslator;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultUnparsedSqlTranslator implements UnparsedSqlTranslator {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultUnparsedSqlTranslator.class);

    private final List<Pattern> unsupportedCommandPatterns = Lists.mutable.with(
            Pattern.compile("(?i)ALTER\\s+TABLE\\s+(\\w+\\.\\.?)?(\\w+)\\s+ALTER\\s+(COLUMN\\s+)?(\\w+)" +
                    "\\s+SET\\s+GENERATED\\s+BY\\s+DEFAULT")
            , Pattern.compile("(?i)ALTER\\s+TABLE\\s+(\\w+\\.\\.?)?(\\w+)\\s+ALTER\\s+(COLUMN\\s+)?(\\w+)" +
                    "\\s+DROP\\s+IDENTITY"));

    @Override
    public String handleRawFullSql(String string, ChangeInput change) {
        string = this.applyUnsupportedCommandPatterns(string);

        // fix sequence as h2 doesn't allow the AS datatype. so a string like
        // CREATE SEQUENCE MYSEQ AS INTEGER START WITH 1 INCREMENT BY 5
        // is replaced with
        // CREATE SEQUENCE MYSEQ START WITH 1 INCREMENT BY 5
        // remove mxavalue, cycle, and order statements
        string = string.replaceAll("(?i)create[ \t]+sequence", "create sequence");
        if (string.trim().toUpperCase().startsWith("CREATE SEQUENCE")) {
            string = string.replaceAll("(?i)as[ \t]+[a-zA-Z]+[ \t]*[\n\r]*[ \t]*start", "start");
            string = string.replaceAll("(?i)maxvalue[ \t]+[-0-9]+", "");
            string = string.replaceAll("(?i)minvalue[ \t]+[-0-9]+", "");
            string = string.replaceAll("(?i)no[ \t]+maxvalue", "");
            string = string.replaceAll("(?i)no[ \t]+minvalue", "");
            string = string.replaceAll("(?i)no[ \t]+cycle", "");
            string = string.replaceAll("(?i)no[ \t]+order", "");
            string = string.replaceAll("(?i)order", "");
        }

        // private final
//        string = string.replaceAll("(?i)nonclustered\\s+index", " index");
//        string = string.replaceAll("(?i)clustered\\s+index", " index");

        // These are needed for backwards-compatibility
        string = string.replaceAll("(?i)grant.*(insert|select|update|delete|usage|alter|index|references|control).*", "");
        string = string.replaceAll("(?i)revoke.*(insert|select|update|delete|usage|alter|index|references|control).*", "");
        string = string.replaceAll("TO GROUP", "TO");
        string = string.replaceAll("TO USER", "TO");

        return string;
    }

    private String applyUnsupportedCommandPatterns(String string) {
        for (Pattern aSkipPattern : this.unsupportedCommandPatterns) {
            Matcher matcher = aSkipPattern.matcher(string);
            if (matcher.find()) {
                LOG.warn("Skipping Command {}", string);
                return "";
            }
        }
        return string;
    }
}

