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
CREATE PROCEDURE ${name} (OUT mycount INT)
READS SQL DATA
BEGIN ATOMIC
    DECLARE myvar INT;
<#assign objects=(dependenttables![]) + (dependentviews![])>
<#list objects![] as object>
    SELECT count(*) into mycount FROM ${object};
</#list>
<#list dependentsps![] as dependentsp>
    CALL ${dependentsp}(myvar);
</#list>
END
GO
