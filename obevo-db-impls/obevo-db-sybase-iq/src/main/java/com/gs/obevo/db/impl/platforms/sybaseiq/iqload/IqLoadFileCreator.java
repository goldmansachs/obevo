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
package com.gs.obevo.db.impl.platforms.sybaseiq.iqload;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.partition.list.PartitionMutableList;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class IqLoadFileCreator {
    private final DataExtractor dataExtractor;
    private final String tableName;

    /**
     * Mappings that do not provide a default value. these will be written to the IQLoadable file
     */
    private final MutableList<FieldToColumnMapping> mappingsWithoutDefaults;

    /**
     * Mappings thatprovide a "default" (really, override) value. these will NOT be written to the IQLoadable file as
     * IQ's default does not allow a real value - it ONLY takes the value in the load command There cannot be values in
     * the file.
     */
    private final MutableList<FieldToColumnMapping> mappingsWithDefaults;

    private final File iqLoadDir;
    private final String loadFilePrefix;
    private BufferedWriter bw;
    private String colDel = "~@#~";
    private String rowDel = "\n"; // So that the column delimiter is rendered to IQ
    private final ConvertUtilsBean cub = new ConvertUtilsBean();
    private final IqLoadMode iqLoadMode;
    private final String filePathToLoad;
    private final File fileToWrite;

    public IqLoadFileCreator(String tableName, MutableList<FieldToColumnMapping> fieldToColumnMappings, File iqLoadDir,
            String loadFilePrefix, IqLoadMode iqLoadMode, DataExtractor dataExtractor) {
        this.tableName = tableName;

        PartitionMutableList<FieldToColumnMapping> parsedMappings =
                fieldToColumnMappings.partition(Predicates.attributeIsNull(FieldToColumnMapping.defaultValue()));

        this.mappingsWithoutDefaults = parsedMappings.getSelected();
        this.mappingsWithDefaults = parsedMappings.getRejected();
        this.iqLoadDir = iqLoadDir;
        this.loadFilePrefix = loadFilePrefix;
        this.cub.register(new SybaseIqLoadFieldConverter(), String.class);
        this.iqLoadMode = iqLoadMode;
        this.dataExtractor = dataExtractor;
        this.fileToWrite = new File(this.getFilePath());
        this.filePathToLoad =
                iqLoadMode.isConvertToWindowsFileSyntax() ? this.getFilePath().replace("\\", "\\\\") : this.getFilePath()
                        .replace("\\", "/");
    }

    private String getFilePath() {
        return this.iqLoadDir + "/" + this.loadFilePrefix + "-" + this.tableName + ".txt";
    }

    public String getTableName() {
        return this.tableName;
    }

    public void setColDel(String colDel) {
        this.colDel = colDel;
    }

    public void setRowDel(String rowDel) {
        this.rowDel = rowDel;
    }

    public void openFile() {
        try {
            if (!this.fileToWrite.getParentFile().exists()) {
                if (!this.fileToWrite.getParentFile().mkdirs()) {
                    throw new RuntimeException("Failed to create load dirs " + this.iqLoadDir.getAbsolutePath());
                }
            }
            this.bw = new BufferedWriter(new FileWriter(this.fileToWrite));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeToFile(Object obj) {
        // read the bean contents via reflection and write them to a file
        MutableList<String> text = Lists.mutable.empty();
        for (FieldToColumnMapping mapping : this.mappingsWithoutDefaults) {
            try {
                Object fieldVal = this.dataExtractor.extractValue(obj, mapping.getFieldName());
                String outputString = fieldVal == null ? "(null)" : this.cub.convert(fieldVal);

                if (outputString.contains(this.colDel)) {
                    throw new IllegalArgumentException("Translated string " + outputString
                            + " contains the column delimiter " + this.colDel
                            + "; please choose another column delimiter (otherwise, this will cause issues)");
                }
                text.add(outputString);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        try {
            this.bw.write(text.makeString("", this.colDel, ""));
            this.bw.write(this.rowDel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Currently the bean is the only input we have. In future, we will support hashmap KVP or other forms of entry
     */
    public void writeToFile(Iterable<?> inputs) {
        for (Object obj : inputs) {
            this.writeToFile(obj);
        }
    }

    public void closeFile() {
        IOUtils.closeQuietly(this.bw);
    }

    public String getIdLoadCommand(String schemaName) {

        StringBuilder sb = new StringBuilder();
        if (this.iqLoadMode.isRequiresCoreLoadOptions()) {
            sb.append("set temporary option CORE_Options54 = 1;").append("\n");
        }

        // This is needed as otherwise the \n text will not be recognize by Sybase IQ as an escaped character
        // sb.append("SET TEMPORARY OPTION Escape_character = 'ON';").append("\n");

        sb.append("LOAD TABLE ").append(schemaName).append(".").append(this.tableName).append("\n(");

        // IQ's normal requirement when loading data is to put coldel at the end of
        sb.append(this.mappingsWithoutDefaults.subList(0, this.mappingsWithoutDefaults.size() - 1)
                .collect(convertWithNull(this.colDel)).makeString("", ", ", ", "));
        sb.append(convertWithNull(this.rowDel).valueOf(this.mappingsWithoutDefaults.getLast()));

        if (!this.mappingsWithDefaults.isEmpty()) {
            sb.append(this.mappingsWithDefaults.collect(convertWithDefault(this.cub)).makeString(", ", ", ", ""));
        }
        sb.append(")").append("\n");

        String clientToggleText = this.iqLoadMode.isClientLoadEnabled() ? "CLIENT " : "";

        sb.append("USING ").append(clientToggleText).append(" FILE '").append(this.filePathToLoad).append("' quotes off escapes off\n");
        sb.append(";").append("\n");

        if (this.iqLoadMode.isRequiresCoreLoadOptions()) {
            sb.append("SET TEMPORARY OPTION CORE_Options54 = 0;").append("\n");
        }
        // sb.append("SET TEMPORARY OPTION Escape_character = 'Off';").append("\n");

        System.out.println("load command = " + sb);
        return sb.toString();
    }

    /*
     * set temporary option CORE_Options54 = 1 ;
     * 
     * load table :wrkSchema.:wrkTable (cm_id '~@#~', :loadColList, upd_cd '~@#|') from :file quotes off escapes off
     * notify 1000000;
     * 
     * set temporary option CORE_Options54 = 0 ;
     */

    private static class SybaseIqLoadFieldConverter implements Converter {

        private static final DateTimeFormatter JODA_DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");

        /*
         * this format string is for timestamp objects, which maintain nanosecond precision. Since none of the standard
         * formatters support printing to this precision, we need to write our own format string. To maintain backward
         * compatibility, we require nanosecond precision. As dates in cobra at present have up to 6 digits of
         * nanoseconds, and CDM-based replication joins on these dates to determine which records to OUT_Z To understand
         * this format string, see http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Formatter.html#syntax
         */
        private static final String TIMESTAMP_FORMAT_STRING_WITH_NANOS = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%2$09d";
        private static final String DATE_TIME_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss.SSS";

        private static final DateTimeFormatter JODA_DATETIME_FORMAT = DateTimeFormat
                .forPattern(DATE_TIME_FORMAT_STRING);

        @Override
        public Object convert(@SuppressWarnings("rawtypes") Class clazz, Object arg1) {
            if (arg1 instanceof LocalDate) {
                return JODA_DATE_FORMAT.print((LocalDate) arg1);
            } else if (arg1 instanceof LocalDateTime) {
                return JODA_DATETIME_FORMAT.print((LocalDateTime) arg1);
            } else if (arg1 instanceof Timestamp) {
                return String.format(TIMESTAMP_FORMAT_STRING_WITH_NANOS, arg1, ((Timestamp) arg1).getNanos());
            } else if (arg1 instanceof Date) {

                // we need to create the date format object here, because it is not thread safe.
                DateFormat JDK_DATETIME_FORMAT = new SimpleDateFormat(DATE_TIME_FORMAT_STRING);
                return JDK_DATETIME_FORMAT.format((Date) arg1);
            } else {
                return arg1.toString();
            }
        }
    }

    private static Function<FieldToColumnMapping, String> convertWithNull(final String colDel) {
        return new Function<FieldToColumnMapping, String>() {
            @Override
            public String valueOf(FieldToColumnMapping arg0) {
                if (colDel == null) {
                    return arg0.getColumnName() + " NULL ('(null)')";
                } else {
                    return arg0.getColumnName() + " '" + colDel + "'" + " NULL ('(null)')";
                }
            }
        };
    }

    private static Function<FieldToColumnMapping, String> convertWithDefault(final ConvertUtilsBean cub) {
        return new Function<FieldToColumnMapping, String>() {
            @Override
            public String valueOf(FieldToColumnMapping field) {
                return field.getColumnName() + " DEFAULT '" + cub.convert(field.getDefaultValue()) + "'";
            }
        };
    }
}