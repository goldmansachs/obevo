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

# Multiple Schema Management Overview

Obevo's configuration lets you define multiple logical schemas in a single system-config.xml file. And you are free to
define as many system-config.xml files or logical schemas as you want to for your application.

The question is - how exactly should you map your physical schemas, logical schemas, and application products? This
page will run through some options.


## Base case - one-to-one mapping between logical schema and physical schema and software product

In the simplest case, your production physical schema is mapped to a single software product, which would define a
single logical schema in your system-config.xml. You can then define multiple environments off this single logical
schema, whether with multiple *dbEnvironment* entries in your system-config.xml file, or by tokenizing the
system-config.xml file against different environments (but keeping the same logical schema list).

This is the example given to you in the kata.

If this works for you, then stick with it. The following sections will describe alternate cases, either:

-   A) Mapping multiple logical schemas to the same system-config.xml file (thus managing multiple schemas together)
-   B) Defining object files for a particular physical schema across multiple objects
-   C) Mapping multiple logical schemas to the same physical schema (a rarer use case)


## A) Mapping multiple logical schemas to the same system-config.xml file

A few requirements are needed for a team to consider managing multiple logical schemas in the same system-config.xml
file. If all of the following do not fit for your use case, then consider keeping them separate.


### Schemas are deployed to in the same release cycle

Currently, the smallest unit of work for Obevo is the dbEnvrionment. Hence, each logical schema would get deployed for
such environments.


### Schemas always exist on the same servers in all environments

i.e. If SCHEMA1 and SCHEMA2 are both found in your production environment, then you also expect them to be deployed
together in all other environments

NOTE - this implies that only 1 primary DB type is supported for all environments in your system-config.


### Same password is used to login to all schemas

As one deploy execution would apply to all schemas, it is thus assumed that the same login would apply for all. This is
particularly relevant for:

-   Sybase ASE - since different schemas/databases can have different permissions, thus your login should have the
    deploy rights on all DBs
-   Sybase IQ - though the default mode is to login as the schema user, the password must be the same across all to
    allow the same input command to deploy to all schemas


### (Optional) Circular dependency across schemas

The multiple-schema setup can be useful to handle circular dependency issues across scheams, e.g.

-   Schema1.ObjectA depends on Schema2.ObjectB
-   Schema2.ObjectB depends on Schema1.ObjectC

Let's start with the simpler use cases:

-   If the schema objects do not refer to other schemas, then your setup
    can be done however you choose.
-   If one schema's objects refers to those of another (but not vice
    versa), you can optionally manage it separately (i.e. separate
    system-config.xml files). However, you must execute the deployments
    in order. Though it is no problem to manage them together

If the two schemas did depend on each other, then it would be hard to
manage them separate due to the deploy order requiring deploys back and
forth across the schemas.

Managing the logical schemas together in the same system-config.xml file
will resolve the deploy order issue, as the deploy algorithm can handle
any sort of deploy ordering.



## B) Defining object files for a particular physical schema across multiple objects

A physical schema may be shared by multiple teams. The preference is to
manage these as one logical schema and to deploy together (see the
[Onboarding Guide FAQ](onboarding-guide.html) for justification of
this).

That said - teams may still want to keep the files maintained in
separate modules. If you choose this route, then at build-time, the
various modules need to have their DDLs combined in one place so that
Obevo can execute the deployment.

It is up to teams to decide how to merge across different modules.
Ultimately, this depends on the build and dependency mechanisms that
your team uses, and Obevo is agnostic of the build system teams choose
to use.


## C) Mapping multiple logical schemas to the same physical schema

If in the worst-case the teams that deploy to the same physical schema
still do not want to deploy at the same time and want to maintain their
own objects, then they can do so by simply defining different logical
schemas for their objects.

Those logical schemas can still map to the same physical schema - Obevo
is able to segregate the different logical schemas defined in a physical
schema.

However, note that this involves a lot of complexity in your setup,
notably around if you can truly guarantee that each logical schema is
independent of each other in that physical schema (see the [Onboarding Guide FAQ](onboarding-guide.html) for more info). Thus, we highly
discourage this route, but it is technically possible to implement.
