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

# Error Handling

What do you do when you get a failure during your DB Deployment?

First, let's understand how Obevo behaves when exceptions occur. Then,
let's discuss the problem resolution options.

------------------------------------------------------------------------

<!-- MACRO{toc|fromDepth=0|toDepth=3} -->


## Obevo Error Handling Behavior

### Best-effort execution

For each deployment activity, Obevo executes a set of changes. If a
change is deployed successfully, then that status is marked in the
DeployLog so that following deployments do not try to execute the same
change.

For example:

-   Version 1 of the DB package has changes C1, C2, C3 - assume all
    pertain to the same object. The deployment succeeds and marks all
    those as deployed in the log.
-   If we try another deployment, Obevo will detect no changes, as C1,
    C2, C3 are already marked in the DeployLog
-   Version 2 comes along w/ changes C1, C2, C3, C4, C5, C6, C7 - assume
    all pertain to the same object. It tries to deploy the difference:
    C4, C5, C6, C7.
-   Let's say C4 succeeds and then C5 fails. In that case, C4 will be
    in the DeployLog, C5 will not be, and C6 and C7 will not be (we will
    not even attempt to do C6 or C7)
-   When re-deploying V2, Obevo will see that C1 - C4 are already
    deployed, so it picks up where it left off (C5, C6, C7)

We made special mention in that example that all those changes were in
the same object. What if they were changes in different DB objects?

-   Obevo will do a best effort to deploy all objects even if unrelated
    objects are failed
-   However, if a change fails for a particular object, then no other
    changes for that object will get executed

Replaying the example above from version 2:

-   Version 2 comes along w/ changes C1, C2, C3, C4, C5, C6, C7 - assume
    all pertain to the same object. It tries to deploy the difference:
    C4, C5, C6, C7.
-   Now, let's assume that C4, C5, C6 belong to ObjectA, and C7 belongs
    to ObjectB.
-   Same as above - let's say C4 succeeds and then C5 fails. We will
    not attempt to deploy C6 as it belonged to the same ObjectA as C5.
    However, C7 belongs to a different object, and so we do try to
    deploy that. Let's assume it succeeds.
-   When re-deploying V2, Obevo will see that C1, C2, C3, C4, **and C7**
    are already deployed, so it picks up what is remaining (C5, C6)

Design note: There was some initial debate about this mode (the
alternative is to just fail the whole deployment upon any exception);
but there was a desire for this best-effort behavior as it made it
easier and faster to resolve issues. The check to prevent successive
changes within an object from proceeding was the safety feature that
allowed the best-effort behavior to work well.


### Transactionality Considerations

The above examples assume that each change C1, C2, \... can be executed
atomically. It would be a problem if they weren't. e.g.

-   Obevo only knows about the whole of each change
-   If C1 was executed partially, Obevo will try to execute the change
    from the beginning
-   Depending on how the change is written, its execution could end up
    failing

This proves particularly interesting for DDL changes, as not all DBMS's
may be configured to allow transactional execution of DDLs. Hence, if
you need to perform a set of actions on a particular object, you should
attempt to break it up in as fine grained change statements as possible.

For example: instead of

```
//// CHANGE name=addColumns
ALTER TABLE myTable ADD COLUMN col1 INT
GO
ALTER TABLE myTable ADD COLUMN col2 INT
GO
ALTER TABLE myTable ADD COLUMN col3 INT
GO
```

do:

```
//// CHANGE name=addCol1
ALTER TABLE myTable ADD COLUMN col1 INT
GO
//// CHANGE name=addCol2
ALTER TABLE myTable ADD COLUMN col2 INT
GO
//// CHANGE name=addCol3
ALTER TABLE myTable ADD COLUMN col3 INT
GO
```

Understandably, the finer-grained the changes are, the more clutter that
would exist in your code. This is the trade-off that you should think
about. With Obevo, we strove to make it as easy as possible to split it
up without too much clutter. (e.g. at least you can split this changes
within the same file using some text syntax, instead of having to keep
them in separate files as other DB Deployment tools may have you do)


## Error-handling during deployment

Now that we understand how Obevo behaves when encountering errors, here
is guidance on how you should handle errors as a user


### Analyze the issue / Error Code Lookup

Start w/ analyzing the exception message. Most databases will give error
messages with a code and a message; use those as hints. e.g. googling
[\"db2 error code\[yourErrorCode\]\"](https://www.google.com/?gws_rd=ssl#q=db2+error+code+204)
will usually find you a page linking to the error description on IBM's page.

Once you've diagnosed an issue, you have a couple options:


### Option 1 - fix the underlying environment and rerun

At times, the error may be environmental and not something controllable
from your DB code, e.g. users/groups not having been setup correctly,
running out of disk space, and so on.

In such cases, fix the underlying issue and rerun the deployment. Per
the Error Handling behavior description above, the deployment will pick
up where it left off.

If this happens to fix the issue, then great. Otherwise, we move on to
Option 2



### Option 2 - rollback or rollforward

This deserves its own page - see the [Rollback Page](rollback.html) for more information.
