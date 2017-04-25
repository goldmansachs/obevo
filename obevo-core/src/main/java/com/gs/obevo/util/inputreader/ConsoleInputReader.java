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
package com.gs.obevo.util.inputreader;

import java.io.Console;
import java.util.Scanner;

import org.apache.commons.lang.SystemUtils;

/**
 * Ideally, we can just use java.io.Console. However, this does not work from Eclipse
 */
public class ConsoleInputReader implements UserInputReader {
    private final Scanner userInputReader = new Scanner(System.in);

    @Override
    public String readLine(String promptMessage) {
        if (this.isWindows()) {
            return this.userInputReader.nextLine();
        } else {
            return getConsole(promptMessage).readLine();
        }
    }

    @Override
    public String readPassword(String promptMessage) {
        if (isWindows()) {
            return this.userInputReader.nextLine();
        } else {
            return String.valueOf(getConsole("password").readPassword());
        }
    }

    private Console getConsole(String readType) {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Attempting to fetch System.console() to read in [" + readType + "], but the System.console() is not available in your environment. Try entering in your login via the input arguments or use the -noPrompt argument");
        }

        return console;
    }

    private boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }
}
