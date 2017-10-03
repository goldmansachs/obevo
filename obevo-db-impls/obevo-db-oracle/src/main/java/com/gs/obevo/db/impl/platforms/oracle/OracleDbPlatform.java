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
package com.gs.obevo.db.impl.platforms.oracle;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.DbChangeTypeImpl;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.impl.platforms.AbstractDbPlatform;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.StringFunctions;

/**
 * Oracle DBMS platform implementation.
 */
public class OracleDbPlatform extends AbstractDbPlatform {
    public OracleDbPlatform() {
        super("ORACLE");

        // See Oracle doc for why we set the J2EE13Compliant property (namely to avoid the non-standard Java types returned in the SQL calls)
        // https://docs.oracle.com/cd/B28359_01/java.111/b31224/datacc.htm
        System.setProperty("oracle.jdbc.J2EE13Compliant", "true");
    }

    @Override
    public Class<? extends DeployerAppContext> initializeAppContextBuilderClass() {
        return OracleAppContext.class;
    }

    @Override
    protected String initializeDefaultDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    protected ImmutableList<ChangeType> initializeChangeTypes() {
        DbChangeType packageBodyType = DbChangeTypeImpl.newDbChangeType(ChangeType.PACKAGE_BODY, true, 60, "PACKAGE BODY").setDirectoryName("package_body").build();
        DbChangeType packageType = DbChangeTypeImpl.newDbChangeType(ChangeType.PACKAGE_STR, true, 11, "PACKAGE").setDirectoryName("packages").setBodyChangeType(packageBodyType).build();
        return super.initializeChangeTypes().toList()
                .with(packageType)
                .with(packageBodyType)
                .toImmutable();
    }

    @Override
    protected String getGrantTargetTypeStrDbSpecific(GrantTargetType grantTargetType) {
        // Oracle doesn't require grant target type keyword
        return "";
    }

    @Override
    public Function<String, String> convertDbObjectName() {
        return StringFunctions.toUpperCase();
    }

    @Override
    public String getTimestampType() {
        return "TIMESTAMP";
    }

    @Override
    public String getBigIntType() {
        return "NUMBER(19)";  // Oracle doesn't support bigint as of version 11; sticking w/ NUMBER for compatibility across versions: https://docs.oracle.com/cd/B19306_01/gateways.102/b14270/apa.htm
    }

    @Override
    public AbstractDdlReveng getDdlReveng() {
        return new OracleReveng();
    }
}
