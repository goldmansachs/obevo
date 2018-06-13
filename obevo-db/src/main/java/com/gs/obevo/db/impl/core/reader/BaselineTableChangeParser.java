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
package com.gs.obevo.db.impl.core.reader;

import java.util.regex.Matcher;

import com.gs.obevo.api.appdata.ChangeInput;
import com.gs.obevo.api.appdata.ChangeKey;
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.impl.core.util.RegexpPatterns;
import com.gs.obevo.impl.reader.DbChangeFileParser;
import com.gs.obevo.impl.util.MultiLineStringSplitter;
import com.gs.obevo.util.hash.DbChangeHashStrategy;
import com.gs.obevo.util.hash.OldWhitespaceAgnosticDbChangeHashStrategy;
import com.gs.obevo.util.vfs.FileObject;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;

public class BaselineTableChangeParser implements DbChangeFileParser {
    private final DbChangeHashStrategy contentHashStrategy = new OldWhitespaceAgnosticDbChangeHashStrategy();
    private final ChangeType fkChangeType;
    private final ChangeType triggerChangeType;

    public BaselineTableChangeParser(ChangeType fkChangeType, ChangeType triggerChangeType) {
        this.fkChangeType = fkChangeType;
        this.triggerChangeType = triggerChangeType;
    }

    @Override
    public ImmutableList<ChangeInput> value(final ChangeType defaultChangeType, final FileObject file, String fileContent, final String objectName, final String schema, TextMarkupDocumentSection packageMetadata) {
        MutableList<String> sqls = MultiLineStringSplitter.createSplitterOnSpaceAndLine("GO").valueOf(fileContent);
        sqls = sqls.reject(Predicates.attributePredicate(StringFunctions.trim(), StringPredicates.empty()));

        MutableList<String> sortedSqls = this.sortSqls(sqls);

        MutableList<ChangeInput> changes = sortedSqls.zipWithIndex().collect(
                new Function<Pair<String, Integer>, ChangeInput>() {
                    @Override
                    public ChangeInput valueOf(Pair<String, Integer> object) {
                        String content = object.getOne();
                        int index = object.getTwo();

                        ChangeType changeType = getChangeType(content, defaultChangeType);

                        String changeName = "baseline-change-" + index;
                        boolean active = true;
                        String rollbackIfAlreadyDeployedCommand = null;
                        String rollbackContent = null;

                        ChangeInput change = new ChangeInput(false);
                        change.setChangeKey(new ChangeKey(schema, changeType, objectName, changeName));
                        change.setOrder(index);
                        change.setContentHash(contentHashStrategy.hashContent(content));
                        change.setContent(content);
                        change.setRollbackIfAlreadyDeployedContent(rollbackIfAlreadyDeployedCommand);
                        change.setActive(active);
                        change.setRollbackContent(rollbackContent);
                        change.setFileLocation(file);
                        return change;
                    }
                });

        return changes.toImmutable();
    }

    private ChangeType getChangeType(String content, ChangeType defaultChangeType) {
        Matcher matcher;
        matcher = RegexpPatterns.fkPattern.matcher(content);
        if (matcher.find()) {
            return fkChangeType;
        }

        matcher = RegexpPatterns.triggerPattern.matcher(content);
        if (matcher.find()) {
            return triggerChangeType;
        }

        return defaultChangeType;
    }

    private MutableList<String> sortSqls(MutableList<String> sqls) {
        MutableList<String> orderedSqls = Lists.mutable.empty();
        MutableList<String> fkSqls = Lists.mutable.empty();
        MutableList<String> triggerSqls = Lists.mutable.empty();

        for (String sql : sqls) {
            Matcher matcher = RegexpPatterns.fkPattern.matcher(sql);
            Matcher triggerMatcher = RegexpPatterns.triggerPattern.matcher(sql);
            if (matcher.find()) {
                fkSqls.add(sql);
            } else if (triggerMatcher.find()) {
                triggerSqls.add(sql);
            } else {
                orderedSqls.add(sql);
            }
        }

        orderedSqls.addAll(fkSqls);
        orderedSqls.addAll(triggerSqls);
        return orderedSqls;
    }
}
