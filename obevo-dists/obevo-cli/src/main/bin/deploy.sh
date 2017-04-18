#!/bin/bash
#
# Copyright 2017 Goldman Sachs.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#


if [ -z "$JAVA_HOME" ]
then
    echo "JAVA_HOME variable must be defined"
    exit 1
fi

# *** In Unix environments, for now we'll assume that the JAVA_HOME variable is already set ***
DEPLOYANY_HOME=$(dirname $0)/..

# *** Set DEPLOYANY_CLASSPATH for when we look to read the DB files from the classpath, esp. via the deployWithCp.bat script ***
CLASSPATH=${DEPLOYANY_CLASSPATH}:${DEPLOYANY_HOME}/conf:${DEPLOYANY_HOME}/lib/*

# *** Set DEPLOYANY_LIBRARY_PATH if we need to add any library paths to the execution, e.g. for Sybase IQ client loads ***
LD_LIBRARY_PATH=${DEPLOYANY_LIBRARY_PATH}:${LD_LIBRARY_PATH}

${JAVA_HOME}/bin/java -cp ${CLASSPATH} com.gs.obevo.dist.Main $@
