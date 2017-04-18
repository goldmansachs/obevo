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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Original author - Rob Mendelow
 * This is for reading in credentials in an agnostic manner
 * <p/>
 * The thing is that on Windows, the regular command line, Eclipse command line, and IntelliJ command line have
 * different quirks for reading information
 * <p/>
 * The most consistent way to read in windows is via a dialog box, so here we have this uniform entry point in code to
 * do so
 */
public class CredentialReader {
    private static final Logger LOG = LoggerFactory.getLogger(CredentialReader.class);
    private final UserInputReader dialogInputReader = new DialogInputReader();  // used for reading the credentials

    public Credential getCredential(String argUserId, String argPassword, boolean useKerberosAuth,
            String keytabPath, String defaultUserId, String defaultPassword) {
        Credential credential = new Credential();
        if (argUserId != null) {
            credential.setUsername(argUserId);
        } else if (defaultUserId != null) {
            credential.setUsername(defaultUserId);
        }

        if (argPassword != null) {
            credential.setPassword(argPassword);
        } else if (defaultPassword != null) {
            credential.setPassword(defaultPassword);
        }

        credential.setUseKerberosAuth(useKerberosAuth);
        credential.setKeytabPath(keytabPath);

        if (credential.getUsername() != null && credential.isAuthenticationMethodProvided()) {
            return credential;
        } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            // if linux, don't run dialog, gs deploy will prompt the user
            return this.getCredentialFromConsole(credential);
        } else {
            return this.getCredentialFromDialog(credential);
        }
    }

    /**
     * @return
     */
    private Credential getCredentialFromConsole(Credential credential) {
        Console cons = System.console();

        if (credential.getUsername() == null) {
            LOG.info("Enter your kerberos (or, DB user name if DB is not DACT enabled): ");
            credential.setUsername(cons.readLine());
        }

        if (!credential.isAuthenticationMethodProvided()) {
            char[] passwd = cons.readPassword("%s", "Password:");
            if (passwd != null) {
                credential.setPassword(new String(passwd));
            }
        }
        return credential;
    }

    private Credential getCredentialFromDialog(Credential credential) {
        this.getUserNameFromDialog(credential);
        this.getPasswordFromDialog(credential);

        return credential;
    }

    private void getUserNameFromDialog(Credential credential) {
        if ((credential.getUsername() != null) && (credential.getUsername().trim().length() > 0)) {
            return;
        }

        String userName = this.dialogInputReader
                .readLine("Enter your kerberos (or, DB user name if DB is not DACT enabled): ");
        if (userName == null) {
            System.err.println("No username provided - exiting");
            System.exit(-1);
        } else {
            credential.setUsername(userName);
        }
    }

    private void getPasswordFromDialog(Credential credential) {
        if (credential.isAuthenticationMethodProvided()) {
            return;
        }

        String password = this.dialogInputReader.readPassword("Enter password for " + credential.getUsername());
        if (password == null) {
            System.err.println("No password provided - exiting");
            System.exit(-1);
        } else {
            credential.setPassword(password);
        }
    }
}
