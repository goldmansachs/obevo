<#--

    Copyright 2017 Goldman Sachs.
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->
<systemConfig type="MONGODB">
    <schemas>
    <#list schemas as schema>
        <schema name="${schema}" />
    </#list>
    </schemas>
    <environments>
        <!-- Your production database that you reverse-engineered from; do NOT deploy to this environment for your testing!
        DELETE THIS COMMENT AFTER FINISHING ONBOARDING -->
        <environment name="prod"
        <#if connectionURI??>
            connectionURI="${connectionURI}">
        <#else>
            connectionURI="mongodb://myProdHost.me.com:27017">
        </#if>
        </environment>

        <!-- Please work with this environment instead for your onboarding testing
        DELETE THIS COMMENT AFTER FINISHING ONBOARDING -->
        <environment name="dev1" cleanBuildAllowed="true"
            connectionURI="mongodb://myProdHost.me.com:27017">

            <!-- To specify a different physical schema, either:
            1) Use the dbSchemaPrefix or dbSchemaSuffix attributes in the <environment> element, e.g.
                        <environment dbSchemaSuffix="_dev" ...>
            will result in the physical schema being:
            <#list schemas as schema>
                ${schema} ==> ${schema}_dev
            </#list>

            2) Use the schemaOverrides element below to define the physical schema name directly

            <schemaOverrides>
            <#list schemas as schema>
                <schemaOverride schema="${schema}" overrideValue="your${schema}InDev"/>
            </#list>
            </schemaOverrides>

            DELETE THIS COMMENT AFTER FINISHING ONBOARDING -->


            <!-- Define tokens here if you so choose

            <tokens>
                <token key="key" value="val"/>
                <token key="key2" value="val2"/>
            </tokens>

            DELETE THIS COMMENT AFTER FINISHING ONBOARDING -->

        </environment>
    </environments>
</systemConfig>
