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

# Considerations on Users Applying Versions to their Deploymennts

Obevo's deployment algorithm (described in detail [here](https://github.com/goldmansachs/obevo/blob/master/Obevo_Javasig.pdf)
does not require users to consider their code bases using application versions (e.g. tagging a binary using a semantic
versioning scheme like X.Y.Z, and determining deploy actions based on that). The algorithm is agnostic to that; Obevo
simply looks at a codebase (consisting of incremental and rerunnable changes) to deploy, and calculates the migration to
perform based on the difference of what is in the deploy log. It does not matter where the code base comes from, whether
from a versioned package or unnamed archive or files on the file system.

However, users do have the option to associate their own version numbers on packages deployed via Obevo, and Obevo has
some features that are enabled when this is used. This page will go into more depth on this.


## Brief Review of Deploy Algorithm

Obevo works by tracking deployed _changes_ as a deploy logic, and only incrementally deploying deltas to the target
environment.

For example:

Deploy V1:
* Environment Audit Log shows no changes deployed
* Source Codebase has three changes (INC1, RR2, RR3)
* Obevo calculates the changeset difference as (INC1, RR2, RR3) and thus attempts to deploy each of those
* Upon successful deployment of an individual change, it is recorded to the Environment Audit Log

Deploy V2:
* Environment Audit Log shows three changes (INC1, RR2, RR3) deployed
* Source Codebase has three changes (INC1, RR2', INC4), with RR2' modified from the original RR2
* To summarize the changeset:
   * INC1 remains unchanged, and so that is not included in the changeset
   * RR2' is modified, and so it is included in the changeset
   * RR3 is removed, and so it is added to the changeset as a deletion
   * INC4 is added, and so it is added to the changeset as a deletion

How adds/drops/modifications are handled depends on the object type:
* Incremental changes (i.e. for tables) do not allow modifications and removals of deployments, except for rebaseline use cases
* Rerunnable objects, in contrast, can be modified and dropped
* Both use cases support simple additions

In the example above, the incremental changs are named with the "INC" prefix, likewise fo rerunnable changes and the "RR"
prefix. We will use this example for the following sections.


## Implications on rollback

### Situation without application versioning

For regular deployments, note that only _additions_ of incremental changes are allowed, not removals. Thus, for typical
releases, new incremental changes tend to be added (e.g. Deploy V1 to Deploy V2)

However, consider if a rollback situation arises (e.g. needing to re-deploy V1 after having deployed V2). In that case,
the deployment would appear as follows:

Re-Deploy V1 from V2:
* Environment Audit Log shows three changes (INC1, RR2', INC4) deployed
* Source Codebase has three changes (INC1, RR2, RR3), with RR2 modified from the original RR2'
* To summarize the changeset:
   * INC1 remains unchanged, and so that is not included in the changeset
   * RR2' is modified, and so it is included in the changeset
   * RR3 is added, and so it is added to the changeset
   * INC4 is removed ...

Note that the INC4 change now appears as a deletion. Normally, this change would not be allowed as this is an incremental
change. In Obevo, users can signal that a deployment is intended as a rollback using the -rollback option (see the
[Rollback Documentation](rollback.md) for more details).

However, users may not always have the flexibility to change the deploy command to include -rollback based on the kind
of deployment tooling they have. We now detail ...

### How Obevo's application versioning functionality can help

In the example above, from a human perspective, it seems obvious that V2 is a later version than V1, and that going from
V2 to V1 should be a rollback. How can we tell Obevo to do this?


1) Specify the -productVersion \<versionName> attribute in your deploy call to have the version number stored

```
deploy.sh DEPLOY -sourcePath /my/source/path -productVersion 1.2.3
```

This is an optional parameter that will store the version value as a new field in the DEPLOYEXECUTION audit table.

2) Using this field, Obevo will know that a version deployment is a rollback if:

* A) The version deployed had previously been deployed
* B) There was another version deployed since the original version

For example:

```
deploy.sh DEPLOY -sourcePath /my/source/path -productVersion 1.0.0  # considered a regular forward deploy
deploy.sh DEPLOY -sourcePath /my/source/path -productVersion 1.0.0  # considered a regular forward deploy (no-op in this case)
deploy.sh DEPLOY -sourcePath /my/source/path -productVersion 2.0.0  # considered a regular forward deploy
deploy.sh DEPLOY -sourcePath /my/source/path -productVersion 1.0.0  # rollback deploy
```

How does Obevo do this? By simply checking for the existence of the productVersion in the DEPLOYEXECUTION table.

Note that the actual version naming scheme does not matter; the check is based purely on the values of the DEPLOYEXECUTION
table.

For example:

```
deploy.sh DEPLOY -sourcePath /my/source/path -productVersion myVersionA  # considered a regular forward deploy
deploy.sh DEPLOY -sourcePath /my/source/path -productVersion myVersionB  # considered a regular forward deploy
deploy.sh DEPLOY -sourcePath /my/source/path -productVersion myVersionA  # rollback deploy
```

Given that, note that this requires deploying a previously-named version as is. As it stands today, Obevo will not
understand semantic versioning and will not automatically consider the deployment as a rollback.

```
deploy.sh DEPLOY -sourcePath /my/source/path -productVersion 1.0.0  # considered a regular forward deploy
deploy.sh DEPLOY -sourcePath /my/source/path -productVersion 2.0.0  # considered a regular forward deploy
deploy.sh DEPLOY -sourcePath /my/source/path -productVersion 1.1.0  # considered a regular forward deploy (even though version number is lower)
```

(It would be possible to have Obevo consider semantic versioning for rollbacks. This would have to be an opt-in argument
for users. It would not be a large build, but needs prioritization. Please raise a Github issue to inquire more)


### Handling Branch Releases

Work in progress

Starting use case:
* you deploy v1.0.0 with changes A1, B1, C1
* then you are working on v2.0.0 in development, say with changes A1, B1, C1, D1, E1 in total

Now say we need to release a fix in production, say to add a change A2
* you deploy v1.0.0 with changes A1, B1, C1, A2

What happens the next time you deploy v2.0.0 on that database?

If you have not incorporated A2 into your v2.0.0 branch, then the next deploy of v2.0.0 will attempt to remove A2, which
may not be correct. Hence, any time you do a release off the non-master branch, please ensure that all merges on the
release branch are merged into the master branch.

For example:
* In v2.0.0, add A2, such as: A1, B1, C2, A2, D1, E1

