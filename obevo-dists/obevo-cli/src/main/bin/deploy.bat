@REM
@REM Copyright 2017 Goldman Sachs.
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM

@echo off

IF "%JAVA_HOME%" == "" (
    ECHO JAVA_HOME variable must be defined to run this script
    EXIT /B 1
)

SET OBEVO_HOME=%~dp0\..

REM *** Set OBEVO_CLASSPATH for when we look to read the DB files from the classpath, esp. via the deployWithCp.bat script ***
SET CLASSPATH=%OBEVO_CLASSPATH%;%OBEVO_HOME%\conf;%OBEVO_HOME%\lib\*

REM *** Set OBEVO_LIBRARY_PATH if we need to add any library paths to the execution, e.g. for Sybase IQ client loads ***
SET PATH=%OBEVO_LIBRARY_PATH%;%PATH%

%JAVA_HOME%\bin\java %OBEVO_JAVA_OPTS% -cp %CLASSPATH% com.gs.obevo.dist.Main %*
