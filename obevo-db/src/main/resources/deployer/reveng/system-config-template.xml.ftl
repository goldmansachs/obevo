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
<dbSystemConfig type="${platform}">
    <schemas>
    <#list schemas as schema>
        <schema name="${schema}" />
    </#list>
    </schemas>
    <environments>
        <!-- Your production database that you reverse-engineered from; do NOT deploy to this environment for your testing!
        DELETE THIS COMMENT AFTER FINISHING ONBOARDING -->
        <dbEnvironment name="prod"
        <#if jdbcUrl??>
            jdbcUrl="${jdbcUrl}">
        <#elseif dbHost?? && dbPort??>
            dbHost="${dbHost}" dbPort="${dbPort}"<#if dbServer??> dbServer="${dbServer}"</#if>>
        <#elseif dbServer??>
            dbDataSourceName="${dbServer}">
        <#else>
            dbHost="myProdHost.me.com" dbPort="9876" dbServer="myProdServer (if applicable)">
        </#if>
        </dbEnvironment>

        <!-- Please work with this environment instead for your onboarding testing
        DELETE THIS COMMENT AFTER FINISHING ONBOARDING -->
        <dbEnvironment name="dev1" cleanBuildAllowed="true"
        <#if dbHost?? && dbPort??>
            dbHost="myDevHost.me.com" dbPort="1234"<#if dbServer??> dbServer="myServer"</#if>>
        <#elseif dbServer??>
            dbDataSourceName="MyDevDataSource01">
        <#else>
            dbHost="myDevHost.me.com" dbPort="1234" dbServer="myServer (if applicable)">
        </#if>

            <!-- To specify a different physical schema, either:
            1) Use the dbSchemaPrefix or dbSchemaSuffix attributes in the <dbEnvironment> element, e.g.
                        <dbEnvironment dbSchemaSuffix="_dev" ...>
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

        </dbEnvironment>
    </environments>
</dbSystemConfig>
