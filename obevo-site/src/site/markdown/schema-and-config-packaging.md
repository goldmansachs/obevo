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

# Separate Schema and Config Packaging

The simplest usage of Obevo is to specify a sourcePath argument that houses both your system-config.xml file and the
schema object files underneath the same containing directory.

Some use cases may require the two to be housed separately, for example:

* A Maven module including schema files within a Java module for unit testing purposes, but wanting to specify the full
  tokenized configs separately
* Including the schema files inside a Kubernetes / Docker container, but providing the config separately for security reasons.


Obevo provides a way to define this separately by leveraging the Java classpath.


### How to implement

Step 1: package your schema files in a subfolder within your package archive.

e.g. in the following example, the root of your schema files is the "myfiles" folder

```
myjar.jar:
/myfiles/myschema/table/tab1.sql
/myfiles/myschema/table/tab2.sql
/myfiles/myschema2/view/view1.sql
```


Step 2: Specify the sourceDirs attribute inside your system-config with the directory chosen previously, e.g.

```
<dbSystemConfig type="YOURTYPE" sourceDirs="myfiles" ...>
```


Step 3: Execute your deployment by specifying -sourcePath to point to your system-config file and your schema package in
your classpath.

If you are using the command line API, you can do this in two ways:

1) Set the OBEVO_CLASSPATH variable to include this path
2) Invoke the deployWithCp.sh script in place of deploy.sh, which simply will set the OBEVO_CLASSPATH variable for you.
The first argument is to pass in the location of your jar, and the rest of the arguments are forwarded to deploy.sh
e.g.

```
OBEVO_CLASSPATH=/home/yourPackage/yourSchemaFiles.jar
$OBEVO_HOME/bin/deploy.sh -sourcePath /home/yourconfig/system-config.xml

# or

$OBEVO_HOME/bin/deployWithCp.sh /home/yourPackage/yourSchemaFiles.jar -sourcePath /home/yourconfig/system-config.xml
```

You can assemble multiple packages and multiple folder names in this manner. Use the classpath-delimiter specific for
your O/S (e.g. : for Unix, ; for Windows) for the classpath variable, and comma to separate the sourceDirs argument
