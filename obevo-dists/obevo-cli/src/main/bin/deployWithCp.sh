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


DEPLOYANY_HOME=$(dirname $0)/..
DEPLOYANY_CLASSPATH=$1
export DEPLOYANY_CLASSPATH

echo Setting DEPLOYANY_CLASSPATH variable as ${DEPLOYANY_CLASSPATH}

REST_OF_ARGS=${*:2}

# *** Now delegating to the full script
${DEPLOYANY_HOME}/bin/deploy.sh ${REST_OF_ARGS}
