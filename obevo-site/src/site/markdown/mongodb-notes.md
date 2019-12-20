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

# MongoDB Notes

MongoDB is not an RDBMS and is typically used in a "schemaless" manner. Hence, it may not seem like a natural fit for a
structured database deployment tool.

However, some teams may have the desire to script and deploy changes to their Mongo instance in a controlled manner, e.g.
for index updates or data modifications.

As such, we provide support for it in Obevo to allow for a similar user experience
for those that also use databases. MongoDB was also Obevo's first attempt at supporting a NoSQL platform, and as such is
a proof of concept that the object-based file structure pattern is a generic setup that can be applied to various platform
types.


## Configuration
The configuration and file setup for MongoDB mostly follows the pattern described in the [DB Project Structure](db-project-structure.html)
page. Here we highlight the differences.

### Runtime: "mongo" required in PATH

Obevo executes Mongo statements by executing the "mongo" command line shell. Hence, this command needs to be set in the
environment PATH before invoking Obevo.

Executing deployments via the native Java API is not yet supported, but we are looking into this in the future.


### system-config.xml file

Renamings:
* dbSystemConfig can be referred to as systemConfig; dbSystemConfig is also readable for backwards-compatibility purposes
* dbEnvironment can be referred to as environment; dbSystemConfig is also readable for backwards-compatibility purposes

Connection parameters to specify are the host and port where the MongoDb instance resides.

Permission schemes, grants, and users are not supported.

Here is an example configuration file:

```
<systemConfig type="MONGODB">
    <schemas>
        <schema name="MYSCHEMA" />
    </schemas>
    <environments>
        <environment name="test1" host="localhost" port="10000" />
        <environment name="test2" host="localhost" port="10001">
            <schemaOverrides>
                <schemaOverride schema="MYSCHEMA" overrideValue="MYSCHEMA_TEST2"/>
            </schemaOverrides>
            <tokens>
                <token key="key" value="val"/>
            </tokens>
        </environment>
    </environments>
</systemConfig>
```


### Available Object Types

See [this link](https://github.com/goldmansachs/obevo/tree/master/obevo-mongodb/src/test/resources/platforms/mongodb/step1/schema1)
for reference examples.

All source is read as Javascript files with extension *.js

##### /collection

Incremental object type that contains information for collection definitions, e.g. indexes.

##### /migration

Data migrations are also possible as described here: [migration type](db-project-structure.html#Ad-hoc_data_migrations)
