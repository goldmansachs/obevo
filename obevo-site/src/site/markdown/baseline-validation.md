<!--

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

# Baseline Validation

You can use the baseline comparison feature to validate that your CHANGEs match a baseline definition of your objects
(i.e. verifying that the cumulative set of alters in your table changes correspond to some full "create table" statement
as a reference).

### How to use it:

If you provide a file w/ ends with .baseline.\<extension\>, this counts
as the baseline file. This is only used when you run Obevo in
validateBaseline mode. the maven plugin in validateBaseline mode (see
the pom example below)

Two ways to run this:

#### 1) Java Code

BaselineValidatorMain
com.gs.obevo.db.apps.baselineutil.BaselineValidatorMain class ?
calculateBaselineBreaks will return a list of breaks ?
validateNoBaselineBreaks is a convenience method to fail if breaks exist


#### 2) Maven plugin

```
<plugin>
    <groupId>com.goldmansachs.obevo</groupId>
    <artifactId>obevo-maven-plugin</artifactId>
    <version>${obevo.version}</version>
    <executions>
        <execution>
            <id>test-db-module</id>
            <goals>
                <goal>test</goal>
            </goals>
            <configuration>
                <env>test</env>
                <user>sa</user>
                <password>deploypass</password>
                <sourcePath>${basedir}/src/main/database</sourcePath>
                <validateBaseline>true</validateBaseline>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### How it works:

-   First runs a -cleanFirst deployment against your environment using
    your regular files (i.e. w/ the CHANGEs)
    -   And queries the table metadata and stores in memory, as to
        remember what was deployed.
-   Then does another -cleanFirst deployment, this time using the
    .baseline. files where it finds them in place of the regular table
    files. (if a table file does not have a corresponding baseline file,
    it just runs the original baseline file)
    -   And queries the table metadata and stores in memory, as to
        remember what was deployed this time
-   Then does a comparison of the metadata objects for the tables. If
    there is a break, then it fails

You can leverage this at build time to ensure that {+}{+}*if you change
your Hibernate/Reladomo/Java model files, that it will stay in sync with
the changes you deploy to your DB*. Nice benefit, huh?

(yes, it would be nice if the tool went a step further and figured out
the alter commands for you and you never saw them. That is the holy
grail...)
