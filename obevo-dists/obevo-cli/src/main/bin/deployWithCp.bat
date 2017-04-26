@REM
@REM Copyright 2017 Goldman Sachs.
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM     http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM

@echo off

SET OBEVO_HOME=%~dp0\..
SET OBEVO_CLASSPATH=%1

ECHO Setting OBEVO_CLASSPATH variable as %OBEVO_CLASSPATH%

REM *** Extracting the classpath from the args ***
SET REST_OF_ARGS=
SHIFT
:loop1
IF "%1"=="" GOTO after_loop
SET REST_OF_ARGS=%REST_OF_ARGS% %1
SHIFT
GOTO loop1
:after_loop

REM *** Now delegating to the full script
%OBEVO_HOME%\bin\deploy.bat %REST_OF_ARGS%
