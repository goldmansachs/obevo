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
package com.gs.obevo.util.inputreader;

public class Credential {
    private String username;
    private String password;
    private boolean useKerberosAuth;
    private String keytabPath;

    public Credential() {
    }

    public Credential(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        if (this.password == null) {
            throw new IllegalArgumentException(
                    "Password requested but does not exist. If you are using kerberos authentication ["
                            + this.useKerberosAuth
                            + "], then the infra code needs to be updated to use the kerberos infra as well. Please contact the obevo team");
        }
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUseKerberosAuth() {
        return this.useKerberosAuth;
    }

    public void setUseKerberosAuth(boolean useKerberosAuth) {
        this.useKerberosAuth = useKerberosAuth;
    }

    public boolean isAuthenticationMethodProvided() {
        return this.useKerberosAuth || this.password != null || this.keytabPath != null;
    }

    public String getKeytabPath() {
        return keytabPath;
    }

    public void setKeytabPath(String keytabPath) {
        this.keytabPath = keytabPath;
    }

    @Override
    public String toString() {
        String passwordDebugText = password != null ? "{passwordHiddenButPresent}" : "{passwordNotPresent}";
        return "Credential{" +
                "username='" + username + '\'' +
                ", password='" + passwordDebugText + '\'' +
                ", useKerberosAuth=" + useKerberosAuth +
                ", keytabPath='" + keytabPath + '\'' +
                '}';
    }
}
