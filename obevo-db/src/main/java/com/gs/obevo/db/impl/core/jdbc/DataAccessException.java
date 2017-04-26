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
package com.gs.obevo.db.impl.core.jdbc;

import java.sql.SQLException;

import org.apache.commons.lang.exception.ExceptionUtils;

public class DataAccessException extends RuntimeException {
    /**
     * Constructor for DataAccessException.
     *
     * @param msg the detail message
     */
    public DataAccessException(String msg) {
        super(msg);
    }

    public DataAccessException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor for DataAccessException.
     *
     * @param msg   the detail message
     * @param cause the root cause (usually from using a underlying
     *              data access API such as JDBC)
     */
    public DataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Retrieve the innermost cause of this exception, if any.
     *
     * @return the innermost exception, or {@code null} if none
     * @since 2.0
     */
    public Throwable getRootCause() {
        Throwable rootCause = null;
        Throwable cause = this.getCause();
        while (cause != null && cause != rootCause) {
            rootCause = cause;
            cause = cause.getCause();
        }
        return rootCause;
    }

    @Override
    public String toString() {
        Throwable rootException = this.getCause() instanceof SQLException
                ? ((SQLException) this.getCause()).getNextException()
                : this.getRootCause();

        if (rootException == null) {
            return ExceptionUtils.getFullStackTrace(this.getCause());
        } else {
            return ExceptionUtils.getFullStackTrace(this.getCause())
                    + "\nWith Root Cause: " + ExceptionUtils.getFullStackTrace(rootException);
        }
    }
}
