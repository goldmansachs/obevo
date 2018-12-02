Note: this page is for Obevo-project developers, not users.

# Releasing Obevo

Simply create a tag in Github named with a semantic version number (e.g. X.Y.Z).

The build script takes care of the rest (see .travis.yml):
* Builds and tests
* Deploys to Maven Central, Docker Hub, Github Releases page, and the site docs


# Implementation Details on Release

## Maven Build

We have moved to [Maven CI-Friendly Builds](https://maven.apache.org/maven-ci-friendly.html) and away from the Maven versions/release plugin.

Hence, we require Maven >= 3.5.0 to build

## Maven Central Upload

Maven Central upload instructions: see [Sonatype OSSRH Guide](http://central.sonatype.org/pages/ossrh-guide.html) and [deploys via Apache Maven](http://central.sonatype.org/pages/apache-maven.html).
* We do not follow the steps on "Nexus Staging Maven Plugin for Deployment and Release"
* Instead, we use the maven-release-plugin: see the "Performing a Release Deployment with the Maven Release Plugin" section

We integrate that into travis.ci using the example here: https://jakob.soy/blog/2016/maven-central-and-travis/

This requires GPG keys to be created and published to the Travis servers. See here for steps (will add more docs on this soon)
```
gpg --export --armor youremail@yourdomain.com > codesigning.asc
gpg --export-secret-keys --armor youremail@yourdomain.com >> codesigning.asc
travis encrypt-file codesigning.asc
```


## Docker Hub Upload
For Docker deploy instructions from Travis, see [here](https://docs.travis-ci.com/user/docker/)

* Note - we chose not to go with the Docker-Github integration as we prefer not to have the Docker build re-execute our
Maven build when the Travis build already does that.



# Dealing with forks in Github

## When committing

Add the main branch as the upstream to your fork
```git remote add upstream https://github.com/goldmansachs/obevo.git```

We request that pull requests are squashed into one commit before the pull request. Use these commands to do that
```
git rebase -i HEAD~3
git push -f
```

Finally, do a pull request in Github.


## When pulling changes from upstream

```
git pull --rebase upstream master
git push -f
```
