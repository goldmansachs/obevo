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
package com.gs.obevo.db.sqlparser.tokenparser;

import org.eclipse.collections.api.block.function.Function;

public class SqlToken {
    private final SqlTokenType tokenType;
    private final String text;

    public SqlToken(SqlTokenType tokenType, String text) {
        this.tokenType = tokenType;
        this.text = text;
    }

    public static final Function<SqlToken, SqlTokenType> TO_TOKEN_TYPE = new Function<SqlToken, SqlTokenType>() {
        @Override
        public SqlTokenType valueOf(SqlToken object) {
            return object.getTokenType();
        }
    };

    public SqlTokenType getTokenType() {
        return this.tokenType;
    }

    public static final Function<SqlToken, String> TO_TEXT = new Function<SqlToken, String>() {
        @Override
        public String valueOf(SqlToken object) {
            return object.getText();
        }
    };

    public String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return "SqlToken{" +
                "tokenType=" + this.tokenType +
                ", text='" + this.text + '\'' +
                '}';
    }
}
