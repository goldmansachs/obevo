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
package com.gs.catodeployany.util;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.gs.catodeployany.compare.CatoBreakExcluder;
import com.gs.catodeployany.compare.CatoComparison;
import com.gs.catodeployany.compare.CatoDataSourceComparator;
import com.gs.catodeployany.compare.CatoProperties;
import com.gs.catodeployany.compare.simple.SimpleCatoProperties;
import com.gs.catodeployany.input.CatoDataSource;
import com.gs.catodeployany.input.db.QueryDataSource;
import com.gs.catodeployany.input.text.DelimitedStreamDataSource;
import com.gs.catodeployany.input.text.FixedStreamDataSource;
import com.gs.catodeployany.output.CatoComparisonWriter;
import com.gs.catodeployany.output.CatoMultiComparisonWriter;
import com.gs.catodeployany.spring.CatoSimpleJavaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatoBaseUtil {

    private static final Logger LOG = LoggerFactory.getLogger(CatoBaseUtil.class);

    public static QueryDataSource createQueryDataSource(String name, String url, String user, String password,
            String query) throws SQLException {
        return createQueryDataSource(name, DriverManager.getConnection(url, user, password), query);
    }

    public static QueryDataSource createQueryDataSource(String name, Connection connection, String query) {
        return new QueryDataSource(name, connection, query);
    }

    public static DelimitedStreamDataSource createDelimitedStreamDataSource(String name, Reader reader, String delimiter) {
        return new DelimitedStreamDataSource(name, reader, delimiter);
    }

    public static DelimitedStreamDataSource createDelimitedStreamDataSource(String name, Reader reader,
            List<String> fields, String delimiter) {
        return new DelimitedStreamDataSource(name, reader, fields, delimiter);
    }

    public static FixedStreamDataSource createFixedStreamDataSource(String name, Reader reader, Object... fieldInput) {
        return new FixedStreamDataSource(name, reader, fieldInput);
    }

    public static CatoComparison compare(String name, CatoDataSource leftDataSource, CatoDataSource rightDataSource,
            List<String> keyFields) {
        return compare(name, leftDataSource, rightDataSource, new SimpleCatoProperties(keyFields));
    }

    public static CatoComparison compare(String name, CatoDataSource leftDataSource, CatoDataSource rightDataSource,
            List<String> keyFields, List<String> excludeFields) {
        return compare(name, leftDataSource, rightDataSource, new SimpleCatoProperties(keyFields, excludeFields));
    }

    public static CatoComparison compare(String name, CatoDataSource leftDataSource, CatoDataSource rightDataSource,
            CatoProperties properties) {
        return compare(name, leftDataSource, rightDataSource, new CatoSimpleJavaConfiguration(properties));
    }

    public static CatoComparison compare(String comparisonName, CatoDataSource leftDataSource,
            CatoDataSource rightDataSource, CatoConfiguration appContext) {
        LOG.info("Beginning comparison of left data source '{}' to right data source '{}'", leftDataSource.getName(),
                rightDataSource.getName());

        CatoProperties properties = appContext.getProperties();
        leftDataSource.setCatoConfiguration(appContext);
        rightDataSource.setCatoConfiguration(appContext);

        CatoDataSourceComparator dataSourceComparator = appContext.dataSourceComparator();
        CatoBreakExcluder breakExcluder = appContext.breakExcluder();

        CatoComparison result = dataSourceComparator.compare(comparisonName, leftDataSource, rightDataSource);

        if (properties.getBreakExcludes() != null && properties.getBreakExcludes().size() != 0) {
            breakExcluder.excludeBreaks(result.getBreaks(), properties.getBreakExcludes());
        }

        LOG.info("Completed comparison of left data source '{}' to right data source '{}'", leftDataSource.getName(),
                rightDataSource.getName());

        return result;
    }

    public static void writeComparison(CatoComparison comparison, CatoComparisonWriter comparisonWriter)
            throws IOException {
        comparisonWriter.writeComparison(comparison);
        comparisonWriter.close();
    }

    public static void writeComparison(Collection<CatoComparison> comparisons,
            CatoMultiComparisonWriter comparisonWriter) throws IOException {
        comparisonWriter.writeComparison(comparisons);
        comparisonWriter.close();
    }

    public static String getDateStr() {
        return new SimpleDateFormat("MM-dd-yyyy HH.mm.ss").format(new Date());
    }
}
