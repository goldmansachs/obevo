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

# Legacy Onboarding Guide for Legacy Systems

This onboarding mode is for systems that are about to be retired and for
which you only want to handle production deployments in a controlled
manner, while eschewing the benefits of testability and having your DB
objects in code as mentioned in the [Onboarding Guide](onboarding-guide.html).

Onboarding to this mode is pretty simple. All you need to do is the same
steps you applied in the kata example, e.g.:

1.  Add your production environment details to your system-config.xml file
2.  Define your DB changes in your source code
3.  Go through to your regular build/package/deploy process and execute
    the deploy w/ the Obevo API

Note that there is no reverse-engineering concept here; i.e. if your
tables already exist in production, then do not try to also create a
"CREATE TABLE" statement in your files as well, just the alters. For
newly-created tables, then it is okay to add the "CREATE TABLE"
statement.

If you do still seek to have your "create table" definitions in code,
please look at the [Onboarding Guide](onboarding-guide.html) and see the
"Existing Systems" options.
