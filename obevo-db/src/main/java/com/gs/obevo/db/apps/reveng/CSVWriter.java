/**
 Copyright 2005 Bytecode Pty Ltd.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
/**
 * This was taken from the net.sf.opencsv:opencsv project, but we
 * added the nullToken property to substitute a different token to be output
 * in place of an empty string.
 */
package com.gs.obevo.db.apps.reveng;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * A very simple CSV writer released under a commercial-friendly license.
 *
 * @author Glen Smith
 */
public class CSVWriter implements Closeable {

    public static final int INITIAL_STRING_SIZE = 128;

    private final Writer rawWriter;

    private final PrintWriter pw;

    private final char separator;

    private final char quotechar;

    private final char escapechar;

    private final String lineEnd;

    private String dateFormatString = "dd-MMM-yyyy";

    private String timeFormatString = "dd-MMM-yyyy HH:mm:ss";

    private String nullToken = "null";

    public void setTimeFormatString(String timeFormatString) {
        this.timeFormatString = timeFormatString;
    }

    public void setDateFormatString(String dateFormatString) {
        this.dateFormatString = dateFormatString;
    }

    public void setNullToken(String nullToken) {
        this.nullToken = nullToken;
    }

    /**
     * The character used for escaping quotes.
     */
    public static final char DEFAULT_ESCAPE_CHARACTER = '"';

    /**
     * The default separator to use if none is supplied to the constructor.
     */
    public static final char DEFAULT_SEPARATOR = ',';

    /**
     * The default quote character to use if none is supplied to the
     * constructor.
     */
    public static final char DEFAULT_QUOTE_CHARACTER = '"';

    /**
     * The quote constant to use when you wish to suppress all quoting.
     */
    public static final char NO_QUOTE_CHARACTER = '\u0000';

    /**
     * The escape constant to use when you wish to suppress all escaping.
     */
    public static final char NO_ESCAPE_CHARACTER = '\u0000';

    /**
     * Default line terminator uses platform encoding.
     */
    public static final String DEFAULT_LINE_END = "\n";

    /**
     * Constructs CSVWriter using a comma for the separator.
     *
     * @param writer the writer to an underlying CSV source.
     */
    public CSVWriter(Writer writer) {
        this(writer, DEFAULT_SEPARATOR);
    }

    /**
     * Constructs CSVWriter with supplied separator.
     *
     * @param writer    the writer to an underlying CSV source.
     * @param separator the delimiter to use for separating entries.
     */
    public CSVWriter(Writer writer, char separator) {
        this(writer, separator, DEFAULT_QUOTE_CHARACTER);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer    the writer to an underlying CSV source.
     * @param separator the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     */
    public CSVWriter(Writer writer, char separator, char quotechar) {
        this(writer, separator, quotechar, DEFAULT_ESCAPE_CHARACTER);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer     the writer to an underlying CSV source.
     * @param separator  the delimiter to use for separating entries
     * @param quotechar  the character to use for quoted elements
     * @param escapechar the character to use for escaping quotechars or escapechars
     */
    public CSVWriter(Writer writer, char separator, char quotechar, char escapechar) {
        this(writer, separator, quotechar, escapechar, DEFAULT_LINE_END);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer    the writer to an underlying CSV source.
     * @param separator the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     * @param lineEnd   the line feed terminator to use
     */
    public CSVWriter(Writer writer, char separator, char quotechar, String lineEnd) {
        this(writer, separator, quotechar, DEFAULT_ESCAPE_CHARACTER, lineEnd);
    }

    /**
     * Constructs CSVWriter with supplied separator, quote char, escape char and line ending.
     *
     * @param writer     the writer to an underlying CSV source.
     * @param separator  the delimiter to use for separating entries
     * @param quotechar  the character to use for quoted elements
     * @param escapechar the character to use for escaping quotechars or escapechars
     * @param lineEnd    the line feed terminator to use
     */
    public CSVWriter(Writer writer, char separator, char quotechar, char escapechar, String lineEnd) {
        this.rawWriter = writer;
        this.pw = new PrintWriter(writer);
        this.separator = separator;
        this.quotechar = quotechar;
        this.escapechar = escapechar;
        this.lineEnd = lineEnd;
    }

    /**
     * Writes the entire list to a CSV file. The list is assumed to be a
     * String[]
     *
     * @param allLines a List of String[], with each String[] representing a line of
     *                 the file.
     */
    public void writeAll(List<String[]> allLines) {
        for (String[] line : allLines) {
            this.writeNext(line);
        }
    }

    protected void writeColumnNames(ResultSetMetaData metadata)
            throws SQLException {

        int columnCount = metadata.getColumnCount();

        String[] nextLine = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            nextLine[i] = metadata.getColumnName(i + 1);
        }
        this.writeNext(nextLine);
    }

    /**
     * Writes the entire ResultSet to a CSV file.
     *
     * The caller is responsible for closing the ResultSet.
     *
     * @param rs                 the recordset to write
     * @param includeColumnNames true if you want column names in the output, false otherwise
     * @throws java.io.IOException   thrown by getColumnValue
     * @throws java.sql.SQLException thrown by getColumnValue
     */
    public void writeAll(java.sql.ResultSet rs, boolean includeColumnNames) throws SQLException, IOException {

        ResultSetMetaData metadata = rs.getMetaData();

        if (includeColumnNames) {
            this.writeColumnNames(metadata);
        }

        int columnCount = metadata.getColumnCount();

        while (rs.next()) {
            String[] nextLine = new String[columnCount];

            for (int i = 0; i < columnCount; i++) {
                nextLine[i] = this.getColumnValue(rs, metadata.getColumnType(i + 1), i + 1);
            }

            this.writeNext(nextLine);
        }
    }

    private String getColumnValue(ResultSet rs, int colType, int colIndex)
            throws SQLException, IOException {

        // SHANT change
        // String value = "";
        String value = null;

        switch (colType) {
        case Types.BIT:
        case Types.JAVA_OBJECT:
            Object obj = rs.getObject(colIndex);
            if (obj != null) {
                value = String.valueOf(obj);
            }
            break;
        case Types.BOOLEAN:
            boolean b = rs.getBoolean(colIndex);
            if (!rs.wasNull()) {
                value = Boolean.valueOf(b).toString();
            }
            break;
        case Types.CLOB:
            Clob c = rs.getClob(colIndex);
            if (c != null) {
                value = read(c);
            }
            break;
        case Types.BIGINT:
            long lv = rs.getLong(colIndex);
            if (!rs.wasNull()) {
                value = Long.toString(lv);
            }
            break;
        case Types.DECIMAL:
        case Types.DOUBLE:
        case Types.FLOAT:
        case Types.REAL:
        case Types.NUMERIC:
            BigDecimal bd = rs.getBigDecimal(colIndex);
            if (bd != null) {
                value = bd.toString();
            }
            break;
        case Types.INTEGER:
        case Types.TINYINT:
        case Types.SMALLINT:
            int intValue = rs.getInt(colIndex);
            if (!rs.wasNull()) {
                value = Integer.toString(intValue);
            }
            break;
        case Types.DATE:
            java.sql.Date date = rs.getDate(colIndex);
            if (date != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(this.dateFormatString);
                value = dateFormat.format(date);
            }
            break;
        case Types.TIME:
            Time t = rs.getTime(colIndex);
            if (t != null) {
                value = t.toString();
            }
            break;
        case Types.TIMESTAMP:
            Timestamp tstamp = rs.getTimestamp(colIndex);
            if (tstamp != null) {
                SimpleDateFormat timeFormat = new SimpleDateFormat(this.timeFormatString);
                value = timeFormat.format(tstamp);
            }
            break;
        case Types.LONGVARCHAR:
        case Types.VARCHAR:
        case Types.CHAR:
            value = rs.getString(colIndex);
            break;
        default:
            value = "";
        }

        // SHANT disabled this

        // if (value == null)
        // {
        // value = "";
        // }

        return value;
    }

    private static String read(Clob c) throws SQLException, IOException {
        StringBuilder sb = new StringBuilder((int) c.length());
        Reader r = c.getCharacterStream();
        char[] cbuf = new char[2048];
        int n;
        while ((n = r.read(cbuf, 0, cbuf.length)) != -1) {
            if (n > 0) {
                sb.append(cbuf, 0, n);
            }
        }
        return sb.toString();
    }

    /**
     * Writes the next line to the file.
     *
     * @param nextLine a string array with each comma-separated element as a separate
     *                 entry.
     */
    public void writeNext(String[] nextLine) {

        if (nextLine == null) {
            return;
        }

        StringBuilder sb = new StringBuilder(INITIAL_STRING_SIZE);
        for (int i = 0; i < nextLine.length; i++) {

            if (i != 0) {
                sb.append(this.separator);
            }

            String nextElement = nextLine[i];
            if (nextElement == null) {
                // SHANT
                if (this.nullToken != null) {
                    sb.append(this.nullToken);
                }
                continue;
            }
            if (this.quotechar != NO_QUOTE_CHARACTER) {
                sb.append(this.quotechar);
            }

            sb.append(this.stringContainsSpecialCharacters(nextElement) ? this.processLine(nextElement) : nextElement);

            if (this.quotechar != NO_QUOTE_CHARACTER) {
                sb.append(this.quotechar);
            }
        }

        sb.append(this.lineEnd);
        this.pw.write(sb.toString());
    }

    private boolean stringContainsSpecialCharacters(String line) {
        return line.indexOf(this.quotechar) != -1 || line.indexOf(this.escapechar) != -1;
    }

    protected StringBuilder processLine(String nextElement) {
        StringBuilder sb = new StringBuilder(INITIAL_STRING_SIZE);
        for (int j = 0; j < nextElement.length(); j++) {
            char nextChar = nextElement.charAt(j);
            if (this.escapechar != NO_ESCAPE_CHARACTER && nextChar == this.quotechar) {
                sb.append(this.escapechar).append(nextChar);
            } else if (this.escapechar != NO_ESCAPE_CHARACTER && nextChar == this.escapechar) {
                sb.append(this.escapechar).append(nextChar);
            } else {
                sb.append(nextChar);
            }
        }

        return sb;
    }

    /**
     * Flush underlying stream to writer.
     */
    public void flush() {

        this.pw.flush();
    }

    /**
     * Close the underlying stream writer flushing any buffered content.
     *
     * @throws IOException if bad things happen
     */
    public void close() throws IOException {
        this.flush();
        this.pw.close();
        this.rawWriter.close();
    }
}
