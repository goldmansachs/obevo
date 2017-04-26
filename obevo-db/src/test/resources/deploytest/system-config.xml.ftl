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
<dbSystemConfig sourceDirs="${sourceDir}"
<#list (sysattrs!{})?keys as sysattr>
    ${sysattr}="${sysattrs[sysattr]}"
</#list>
>
    <schemas>
<#list (logicalSchemas!{})?keys as logicalSchema>
        <schema name="${logicalSchemas[logicalSchema]}" />
</#list>
    </schemas>
    <environments>
        <dbEnvironment name="test" cleanBuildAllowed="true"
                       <#if dbDataSourceName??>dbDataSourceName="${dbDataSourceName}"<#elseif dbServer??>dbServer="${dbServer}"<#elseif jdbcUrl??>jdbcUrl="${jdbcUrl}"</#if>
                       <#if driver??>driverClass="${driver}"</#if>
        <#list (envattrs!{})?keys as envattr>
                        ${envattr}="${envattrs[envattr]}"
        </#list>
        >
            <schemaOverrides>
            <#list (logicalSchemas!{})?keys as logicalSchema>
                <schemaOverride schema="${logicalSchemas[logicalSchema]}" overrideValue="${schemas[logicalSchema]}" />
            </#list>
            </schemaOverrides>
        </dbEnvironment>
    </environments>
    <permissions>
    <#if group?? || rogroup?? || user??>
        <#assign groupEntry="groups=\"${group}\"">
        <#assign roGroupEntry="groups=\"${rogroup}\"">
        <#assign userEntry="users=\"${user}\"">
        <permission scheme="TABLE_RO">
            <grant ${roGroupEntry} ${userEntry} privileges="SELECT, DELETE" />
        </permission>
        <permission scheme="TABLE_RW">
            <grant ${roGroupEntry} privileges="SELECT, DELETE" />
            <grant ${groupEntry} privileges="SELECT, INSERT, DELETE" />
        </permission>
        <permission scheme="VIEW">
            <grant ${roGroupEntry} privileges="SELECT" />
            <grant ${groupEntry} privileges="SELECT" />
        </permission>
        <permission scheme="SEQUENCE">
            <grant ${groupEntry} privileges="SELECT, ALTER" />
        </permission>
        <permission scheme="FUNCTION">
            <grant ${groupEntry} privileges="EXECUTE" />
        </permission>
        <permission scheme="SP">
            <grant ${groupEntry} privileges="EXECUTE" />
        </permission>
    </#if>
    </permissions>
</dbSystemConfig>
