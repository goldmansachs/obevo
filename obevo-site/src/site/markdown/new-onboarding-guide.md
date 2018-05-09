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

# New System Onboarding Guide

If you are able to choose this path for onboarding, ***you are in luck -
it is the easiest path and you get to use all the features of Obevo!***

Technically, all you need to do is the same steps you applied in the
kata example, e.g.:

1.  Add your production environment details to your system-config.xml file
2.  Define your DB objects in your source code
3.  Go through to your regular build/package/deploy process and execute
    the deploy w/ the Obevo API

Some more recommended things to do as you define your new code base:


## More Recommended Steps To Do For All Full-Onboarding Systems (New Or Existing)


### See the DB Project Best Practices Section

Click [here](db-project-best-practices.html)


### Consider if you want to manage multiple schemas in the same system-config.xml file

This question only applies if you do have multiple schemas in your
production environments that you are trying to onboard.

See [the doc](multiple-schema-management.html) for more information.


### Clean up any existing non-production environments

If your production environment is going to have a fresh start, your
non-production environments should too. If you had any UAT or Dev
environments that had tables not deployed using Obevo, seek to clean
those out so that subsequent activity on those environments can safely
match what you expect out of your code.


### Knowledge Transition to your team

You can use this document as a reference
