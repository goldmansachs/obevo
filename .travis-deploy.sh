#!/bin/bash
#
# Copyright 2017 Goldman Sachs.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
set -e
echo "Deploying to Maven Central Repo with Maven Opts $MAVEN_OPTS"

# Maven deploy steps done with help from:
# - https://github.com/stefanbirkner/travis-deploy-to-maven-central
# - https://central.sonatype.org/pages/apache-maven.html
# - https://central.sonatype.org/pages/working-with-pgp-signatures.html

# the following openssl line is taken from the "travis encrypt" command
openssl aes-256-cbc -K $encrypted_a2f0f379c735_key -iv $encrypted_a2f0f379c735_iv -in deploy/signingkey.asc.enc -out deploy/signingkey.asc -d

gpg --fast-import deploy/signingkey.asc
cp deploy/.travis.maven.settings.xml $HOME/.m2/settings.xml
mvn -B -DskipTests -P release deploy

# Uncomment when docker hub creds are sorted
#echo "Deploying to Docker Hub"

# Note - docker recommends passing in the password via --password-stdin for security purposes.
# See https://docs.docker.com/engine/reference/commandline/login/#provide-a-password-using-stdin
echo "$SONATYPE_PASSWORD" | docker login -u "$SONATYPE_USERNAME" --password-stdin

#if [[ "$VERSION" != "*-SNAPSHOT" ]];
#then
#    echo "Applying latest tag to fixed release version $VERSION"
#    docker tag shantstepanian/obevo:$VERSION shantstepanian/obevo:latest
#fi
#docker push shantstepanian/obevo
