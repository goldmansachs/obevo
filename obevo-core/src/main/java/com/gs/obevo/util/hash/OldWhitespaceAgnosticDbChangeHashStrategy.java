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
package com.gs.obevo.util.hash;

import com.gs.obevo.util.DAStringUtil;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * TODO Should eventually replace this class w/ {@link WhitespaceAgnosticDbChangeHashStrategy}. Though we need a way to
 * replace the persisted hashes in the DB.
 */
public class OldWhitespaceAgnosticDbChangeHashStrategy implements DbChangeHashStrategy {
    @Override
    public String hashContent(String content) {
        return DigestUtils.md5Hex(this.normalizeFileContentsForHashing(content));
    }

    String normalizeFileContentsForHashing(String content) {
        return DAStringUtil.normalizeWhiteSpaceFromStringOld(content);
    }
}
