# Releasing Obevo

## Release Steps

Run the maven release plugin to prepare the build. This will invoke the tests.

```mvn release:clean release:prepare```

Once succeeded, perform the release to upload the artifacts to maven central. Watch out for the GPG keychain password prompt.

```mvn release:perform```


## Implementation Details

(We document this here only for knowledge purposes)

Follow the instructions on the [Sonatype OSSRH Guide](http://central.sonatype.org/pages/ossrh-guide.html).

Include the drilldown page that [deploys via Apache Maven](http://central.sonatype.org/pages/apache-maven.html).
* We do not follow the steps on "Nexus Staging Maven Plugin for Deployment and Release"
* Instead, we use the maven-release-plugin: see the "Performing a Release Deployment with the Maven Release Plugin" section

Note the "maven-release-plugin" declaration in the parent pom here; see the references to the _release_ profile and the
release profile definition itself.



# Dealing with branches in Github

## When committing

Add the main branch as the upstream to your fork
```git remote add upstream https://github.com/goldmansachs/obevo.git```

We request that pull requests are squashed into one commit before the pull request. Use these commands to do that
```
git rebase -i HEAD~3
git push -f
```

Finally, do a pull reqeust in Github.


## When pulling changes from upstream

```
git pull --rebase upstream master
git push -f
```


# Deploying to remote repos:

To Sonatype via Github - used code here: https://jakob.soy/blog/2016/maven-central-and-travis/
For Docker - https://docs.travis-ci.com/user/docker/

# GPG setup

Shants-MacBook-Pro:obevo shantstepanian$ gpg --export --armor shant.p.stepanian@gmail.com > codesigning.asc
Shants-MacBook-Pro:obevo shantstepanian$ gpg --export-secret-keys --armor shant.p.stepanian@gmail.com >> codesigning.asc
Shants-MacBook-Pro:obevo shantstepanian$ travis encrypt-file codesigning.asc
