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
package com.gs.obevo.db.sqlparser.syntaxparser;

import java.io.StringReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlSyntaxParser {
    private static final Logger LOG = LoggerFactory.getLogger(SqlSyntaxParser.class);

    public UnparseVisitor getParsedValue(String input) {
        SqlParser p = new SqlParser(new StringReader(input));

        ASTCompilationUnit cu;
        try {
            cu = p.CompilationUnit();
        } catch (ParseException e) {
            LOG.debug("Could not parse this SQL w/ ASTs; enable trace-level logging to see the exception: {}", input);
            if (LOG.isTraceEnabled()) {
                LOG.trace("THIS EXCEPTION IS SAFE TO IGNORE IN 99.99% OF CASES - exception from failure to parse SQL {}: {}", input, e);
            }
            return null;
        } catch (TokenMgrError e) {
            throw new RuntimeException("Failed processing input: " + input, e);
        }

        UnparseVisitor visitor = new UnparseVisitor(System.out);
        cu.jjtAccept(visitor, null);

        return visitor;
    }
}
