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

import java.util.regex.Matcher;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.impl.core.util.MultiLineStringSplitter;
import com.gs.obevo.db.impl.core.util.RegexpPatterns;
import com.gs.obevo.util.hash.DbChangeHashStrategy;
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
    private final DbChangeHashStrategy contentHashStrategy;
    private final ChangeType fkChangeType;
    private final ChangeType triggerChangeType;

    public BaselineTableChangeParser(DbChangeHashStrategy contentHashStrategy, ChangeType fkChangeType, ChangeType triggerChangeType) {
        this.contentHashStrategy = contentHashStrategy;
        this.fkChangeType = fkChangeType;
        this.triggerChangeType = triggerChangeType;
    }

    @Override
    public ImmutableList<Change> value(final ChangeType defaultChangeType, final FileObject file, final String schema, TextMarkupDocumentSection packageMetadata) {
        MutableList<String> sqls = MultiLineStringSplitter.createSplitterOnSpaceAndLine("GO").valueOf(file.getStringContent());
        sqls = sqls.reject(Predicates.attributePredicate(StringFunctions.trim(), StringPredicates.empty()));

        MutableList<String> sortedSqls = this.sortSqls(sqls);

        final String objectName = file.getName().getBaseName().split("\\.")[0];

        MutableList<Change> changes = sortedSqls.zipWithIndex().collect(
                new Function<Pair<String, Integer>, Change>() {
                    @Override
                    public Change valueOf(Pair<String, Integer> object) {
                        String content = object.getOne();
                        int index = object.getTwo();

                        ChangeType changeType = getChangeType(content, defaultChangeType);

                        String changeName = "baseline-change-" + index;
                        boolean active = true;
                        String rollbackIfAlreadyDeployedCommand = null;
                        String rollbackContent = null;

                        ChangeIncremental change = new ChangeIncremental(changeType, schema, objectName,
                                changeName, index, BaselineTableChangeParser.this.contentHashStrategy.hashContent(content), content,
                                rollbackIfAlreadyDeployedCommand, active);
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
