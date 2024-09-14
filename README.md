# Gson

Gson is a Java library that can be used to convert Java Objects into their JSON representation. It can also be used to convert a JSON string to an equivalent Java object.
Gson can work with arbitrary Java objects including pre-existing objects that you do not have source-code of.

There are a few open-source projects that can convert Java objects to JSON. However, most of them require that you place Java annotations in your classes; something that you can not do if you do not have access to the source-code. Most also do not fully support the use of Java Generics. Gson considers both of these as very important design goals.

> [!NOTE]\
> Gson is currently in maintenance mode by Google; existing bugs will be fixed, but large new features will likely not be added. If you want to add a new feature, please first search for existing GitHub issues, or create a new one to discuss the feature and get feedback.

> [!IMPORTANT]\
> Gson's main focus is on Java. Using it with other JVM languages such as Kotlin or Scala might work fine in many cases, but language-specific features such as Kotlin's non-`null` types or constructors with default arguments are not supported. This can lead to confusing and incorrect behavior.\
> When using languages other than Java, prefer a JSON library with explicit support for that language.

## Happeo Fork

This is a fork of the official Gson repository that fixes issues we at Happeo find important and allows us to add
features we need.

Notes:
* Our fork requires Java 8+.
* Our fork builds with Java 17+ without restriction.
* We only release the main `gson` module, nothing else.
* We release the source JAR along with code, so no need for a separate JavaDoc JAR.
* We rely on internal Maven repository to provide reasonable degree of build reproducibility.
* We keep up to date with upstream manually, commit by commit, then full merge.
* Tests use Java 17 to run Java record tests while the main code is Java 8 compatible.
* IntelliJ can't handle different JDK version for tests vs. production code, so you can't run e.g. Java record tests in IntelliJ.
* Our Java record support is based on code originally submitted to all major JSON library maintainers by the OpenJDK community(?), which predates the official support and had better support for parmeterized types but leads to major merge conflicts. Trade-offs...

Release process, which assumes updates have been applied to a separate branch:
1. Make local snapshot build: `mvn clean install -pl 'gson' -am -Dproguard.skip -Dbuildinfo.attach=false`
1. Smoke test local snapshot with larger backend services
1. Merge to the main branch: `git checkout master && g merge --ff-only <branch>`
1. Specify the release version: `RELEASE_VERSION=<gson version>-happeo-<revision>`
1. Specify the next snapshot version: `DEVELOPMENT_VERSION=<gson version>-happeo-<revision + 1>-SNAPSHOT`
1. Specify the release tag: `RELEASE_TAG=gson-parent-${RELEASE_VERSION}`
1. Update the POM files to the release version: `mvn versions:set -DnewVersion="${RELEASE_VERSION}" -DgenerateBackupPoms=false -DupdateMatchingVersions=false`
1. Verify the build: `mvn clean verify -pl 'gson' -am -Dproguard.skip`
1. Commit the updated versions: `git commit -qam "[RELEASE] ${RELEASE_VERSION} released"`
1. Tag the release: `git tag -afm "[RELEASE] ${RELEASE_VERSION}" "${RELEASE_TAG}"`
1. Update the POM files to the next snapshot version: `mvn versions:set -DnewVersion="${DEVELOPMENT_VERSION}" -DgenerateBackupPoms=false -DupdateMatchingVersions=false`
1. Verify the build: `mvn clean verify -pl 'gson' -am -Dproguard.skip`
1. Commit the updated versions: `git commit -qam "[RELEASE] ${DEVELOPMENT_VERSION} prepared"`
1. Push the updates to our fork: `git push --follow-tags origin`
1. Checkout the release tag: `git checkout ${RELEASE_TAG}`
1. Deploy the release: `mvn clean deploy -pl 'gson' -am -DskipTests -Dproguard.skip -Dbuildinfo.attach=false -Dinternal.repository.url.base=...`
1. Checkout the main branch: `git checkout master`

## Goals
  * Provide simple `toJson()` and `fromJson()` methods to convert Java objects to JSON and vice-versa
  * Allow pre-existing unmodifiable objects to be converted to and from JSON
  * Extensive support of Java Generics
  * Allow custom representations for objects
  * Support arbitrarily complex objects (with deep inheritance hierarchies and extensive use of generic types)

## Download

Maven:
```xml
<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
  <version>2.11.0-happeo-1</version>
</dependency>
```

[Gson jar downloads](https://maven-badges.herokuapp.com/maven-central/com.google.code.gson/gson) are available from Maven Central.

![Build Status](https://github.com/google/gson/actions/workflows/build.yml/badge.svg)

## Requirements
### Minimum Java version
- Java 8

Despite supporting older Java versions, Gson also provides a JPMS module descriptor (module name `com.google.gson`) for users of Java 9 or newer.

### JPMS dependencies (Java 9+)
These are the optional Java Platform Module System (JPMS) JDK modules which Gson depends on.
This only applies when running Java 9 or newer.

- `java.sql` (optional since Gson 2.8.9)\
When this module is present, Gson provides default adapters for some SQL date and time classes.

- `jdk.unsupported`, respectively class `sun.misc.Unsafe` (optional)\
When this module is present, Gson can use the `Unsafe` class to create instances of classes without no-args constructor.
However, care should be taken when relying on this. `Unsafe` is not available in all environments and its usage has some pitfalls,
see [`GsonBuilder.disableJdkUnsafe()`](https://javadoc.io/doc/com.google.code.gson/gson/latest/com.google.gson/com/google/gson/GsonBuilder.html#disableJdkUnsafe()).

## Documentation
  * [API Javadoc](https://www.javadoc.io/doc/com.google.code.gson/gson): Documentation for the current release
  * [User guide](UserGuide.md): This guide contains examples on how to use Gson in your code
  * [Troubleshooting guide](Troubleshooting.md): Describes how to solve common issues when using Gson
  * [Releases and change log](https://github.com/google/gson/releases): Latest releases and changes in these versions; for older releases see [`CHANGELOG.md`](CHANGELOG.md)
  * [Design document](GsonDesignDocument.md): This document discusses issues we faced while designing Gson. It also includes a comparison of Gson with other Java libraries that can be used for Json conversion

Please use the ['gson' tag on StackOverflow](https://stackoverflow.com/questions/tagged/gson), [GitHub Discussions](https://github.com/google/gson/discussions) or the [google-gson Google group](https://groups.google.com/group/google-gson) to discuss Gson or to post questions.

## Related Content Created by Third Parties
  * [Gson Tutorial](https://www.studytrails.com/java/json/java-google-json-introduction/) by `StudyTrails`
  * [Gson Tutorial Series](https://futurestud.io/tutorials/gson-getting-started-with-java-json-serialization-deserialization) by `Future Studio`
  * [Gson API Report](https://abi-laboratory.pro/java/tracker/timeline/gson/)

## Building

Gson uses Maven to build the project:
```
mvn clean verify
```

JDK 17 or newer is required for building. Newer JDKs are currently not supported for building (but are supported when _using_ Gson).

## Contributing

See the [contributing guide](https://github.com/google/.github/blob/master/CONTRIBUTING.md).\
Please perform a quick search to check if there are already existing issues or pull requests related to your contribution.

Keep in mind that Gson is in maintenance mode. If you want to add a new feature, please first search for existing GitHub issues, or create a new one to discuss the feature and get feedback.

## License

Gson is released under the [Apache 2.0 license](LICENSE).

```
Copyright 2008 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Disclaimer

This is not an officially supported Google product.

## Release

Root POM:

```xml
<properties>
    <artifactory.repository.url.base>…</artifactory.repository.url.base>
</properties>

…

<distributionManagement>
    <repository>
        <id>central</id>
        <name>happeo-releases</name>
        <url>${artifactory.repository.url.base}/libs-release-local</url>
    </repository>
    <snapshotRepository>
        <id>snapshots</id>
        <name>happeo-snapshots</name>
        <url>${artifactory.repository.url.base}/libs-snapshot-local</url>
    </snapshotRepository>
</distributionManagement>
```

Commands:

```shell
$ GSON_VERSION=<release version>
$ git checkout master
$ git merge gson-parent-${GSON_VERSION}
```

Set the release version in the POMs, then:

```shell
$ git commit -qam "[RELEASE] ${GSON_VERSION} released"
$ git tag -afm "[RELEASE] ${GSON_VERSION}" "gson-parent-${GSON_VERSION}"
$ mvn clean deploy
```

Bump to the next snapshot version in the POMs, then:

```shell
$ GSON_VERSION=<next release version>
$ git commit -qam "[RELEASE] ${GSON_VERSION} prepared"
$ git push --follow-tags origin
```
