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
package com.gs.obevo.util;

import java.util.List;

import com.sampullara.cli.Args;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notes on using cli pos:
 * -if you want to use subclasses for args, then make the variables public
 * Note that if you set the variables as private, then it would read it on that class, but
 * not on subclasses due to implementation
 * personally, i prefer using the field + annotation directly over setters (that is too cumbersome)
 */
public class ArgsParser {

    private static final Logger LOG = LoggerFactory.getLogger(ArgsParser.class);
    
    public <T> T parse(String[] args, T inputArgs) {
        try {
            List<String> extraArgs = Args.parse(inputArgs, args);

            if (extraArgs.size() > 0) {
                Args.usage(inputArgs);
                System.err.println("Passed in unnecessary args: " + StringUtils.join(extraArgs, "; "));
                System.exit(-1);
            }
        } catch (IllegalArgumentException exc) {
            Args.usage(inputArgs);
            exc.printStackTrace();
            System.exit(-1);
        }

        LOG.info("Arguments parsed: " + inputArgs.toString());
        
        return inputArgs;
    }
}
