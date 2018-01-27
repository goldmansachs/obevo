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
package com.gs.obevo.db.impl.core.compare.data;

import java.io.File;

import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.dbmetadata.api.DaColumn;
import com.gs.obevo.dbmetadata.api.DaIndex;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.util.ArgsParser;
import com.gs.obevo.util.FileUtilsCobra;
import com.gs.obevocomparer.compare.CatoComparison;
import com.gs.obevocomparer.input.db.QueryDataSource;
import com.gs.obevocomparer.output.html.HTMLComparisonWriter;
import com.gs.obevocomparer.util.CatoBaseUtil;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;

public class DbDataComparisonUtil {

    private final DbPlatform dbPlatform;

    public static void main(String[] args) {
        new DbDataComparisonUtil().execute(new ArgsParser().parse(args, new DbDataComparisonArgs()));
    }

    public DbDataComparisonUtil() {
        try {
            // The classname is hardcoded here via reflection from the original POC implementation, and didn't get a chance
            // to refactor it yet. Code has not been productionized yet
            dbPlatform = (DbPlatform) Class.forName("com.gs.obevo.db.impl.platforms.sybasease.AseDbPlatform").newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("This functinoality is not fully supported; given dialect does not exist; contact team for more info", e);
        }
    }

    private void execute(DbDataComparisonArgs args) {
        try {
            this.execute(DbDataComparisonConfigFactory.createFromProperties(args.getConfigFile()),
                    new File(args.getOutputDir()));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public void execute(DbDataComparisonConfig dbDataComparisonConfig, File outputDir) throws Exception {
        dbDataComparisonConfig.init();

        MutableList<ComparisonSummaryRow> summaries = Lists.mutable.empty();
        for (String table : dbDataComparisonConfig.getInputTables()) {
            if (dbDataComparisonConfig.getExcludedTables().contains(table)) {
                continue;
            }

            ComparisonSummaryRow summary = new ComparisonSummaryRow(table);

            MutableList<ComparisonResult> comparisonResults = Lists.mutable.empty();
            for (ComparisonCommand reconCommand : dbDataComparisonConfig.getComparisonCommands()) {
                comparisonResults.add(this.compare(table, reconCommand));
            }

            MutableList<CatoComparison> reportableComparisons = Lists.mutable.empty();

            for (ComparisonResult comparisonResult : comparisonResults) {
                summary.addResult(comparisonResult.getComparisonCommand().getKey(), comparisonResult);

                if (comparisonResult.getCatoComparison() != null
                        && comparisonResult.getComparisonResultType() == ComparisonResultType.ACTUAL) {
                    reportableComparisons.add(comparisonResult.getCatoComparison());
                }
            }

            if (!reportableComparisons.isEmpty()) {
                String contentFile = outputDir.getPath() + "/comp-results-" + table + "-content.txt";
                String summaryFile = outputDir.getPath() + "/comp-results-" + table + "-summary.txt";
                CatoBaseUtil.writeComparison(reportableComparisons, new HTMLComparisonWriter(contentFile, summaryFile));
            }

            summaries.add(summary);
        }

        MutableList<String> colHeaders = FastList.<String>newList()
                .with("TABLE")
                .withAll(dbDataComparisonConfig.getComparisonCommands().collect(ComparisonCommand.TO_KEY))
                .with("NEEDS_ANALYSIS");

        MutableList<String> outputLines = Lists.mutable.empty();
        outputLines.add(colHeaders.makeString("\"", "\",\"", "\""));
        for (final ComparisonSummaryRow summary : summaries) {
            String regUatSummary;

            MutableList<String> outputValues = dbDataComparisonConfig.getComparisonCommands().collect(
                    new Function<ComparisonCommand, String>() {
                        @Override
                        public String valueOf(ComparisonCommand object) {
                            return summary.getResultForOutput(object.getKey());
                        }
                    });

            outputLines.add(Lists.mutable.with(summary.getTable())
                    .withAll(outputValues)
                    .with(String.valueOf(summary.isNeedsAnalysis()))
                    .makeString("\"", "\",\"", "\""));
        }

        FileUtilsCobra.writeLines(new File(outputDir, "summary-report.csv"), outputLines);
    }

    private ComparisonResult compare(String table, ComparisonCommand comparisonCommand) {
        DbDataSource leftDbDs = comparisonCommand.getLeftDs();
        leftDbDs.init();
        DbDataSource rightDbDs = comparisonCommand.getRightDs();
        rightDbDs.init();

        try {

            DbMetadataManager leftDbMetaManager = dbPlatform.getDbMetadataManager();
            leftDbMetaManager.setDataSource(leftDbDs.getDs());
            DbMetadataManager rightdbMetaManager = dbPlatform.getDbMetadataManager();
            rightdbMetaManager.setDataSource(rightDbDs.getDs());

            DaTable left = leftDbMetaManager.getTableInfo(leftDbDs.getSchema(), table);
            DaTable right = rightdbMetaManager.getTableInfo(rightDbDs.getSchema(), table);

            if (left == null) {
                return new ComparisonResult(comparisonCommand, null, ComparisonResultType.ONLY_ON_RIGHT_SIDE);
            } else if (right == null) {
                return new ComparisonResult(comparisonCommand, null, ComparisonResultType.ONLY_ON_LEFT_SIDE);
            }

            MutableList<String> keyCols = getKeyCols(left);
            if (keyCols.isEmpty()) {
                keyCols = getKeyCols(right);
            }

            QueryDataSource leftCatoDs = CatoBaseUtil.createQueryDataSource(leftDbDs.getName() + "-" + table, leftDbDs
                    .getDs().getConnection(), String.format("select * from %s..%s", leftDbDs.getSchema(), table));
            QueryDataSource rightCatoDs = CatoBaseUtil.createQueryDataSource(rightDbDs.getName() + "-" + table,
                    rightDbDs
                            .getDs().getConnection(), String.format("select * from %s..%s", rightDbDs.getSchema(), table));

            CatoComparison comp = CatoBaseUtil.compare(table + "-" + leftDbDs.getName() + "-" + rightDbDs.getName(),
                    leftCatoDs, rightCatoDs, keyCols);
            return new ComparisonResult(comparisonCommand, comp, isComparisonLegit(comp));
        } catch (Exception exc) {
            return new ComparisonResult(comparisonCommand, null, ComparisonResultType.EXCEPTION);
        }
    }

    private static ComparisonResultType isComparisonLegit(CatoComparison c) {
        if (c.getLeftData().isEmpty() && !c.getRightData().isEmpty()) {
            return ComparisonResultType.ONLY_ON_RIGHT_SIDE;
        } else if (!c.getLeftData().isEmpty() && c.getRightData().isEmpty()) {
            return ComparisonResultType.ONLY_ON_LEFT_SIDE;
        } else if (!c.getBreaks().isEmpty()) {
            return ComparisonResultType.ACTUAL;
        } else {
            return ComparisonResultType.NO_BREAKS;
        }
    }

    private static MutableList<String> getKeyCols(DaTable tableInfo) {
        MutableList<String> keyCols = Lists.mutable.empty();
        DaIndex pk = tableInfo.getPrimaryKey();
        for (DaIndex index : tableInfo.getIndices()) {
            if (index.isUnique()) {
                pk = index;
                break;
            }
        }
        if (pk != null) {
            for (DaColumn col : pk.getColumns()) {
                keyCols.add(col.getName());
            }
        }
        return keyCols;
    }
}
