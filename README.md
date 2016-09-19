Maven Javassist
===============

<a href="https://raw.githubusercontent.com/ArpNetworking/maven-javassist/master/LICENSE">
    <img src="https://img.shields.io/hexpm/l/plug.svg"
         alt="License: Apache 2">
</a>
<a href="https://travis-ci.org/ArpNetworking/maven-javassist/">
    <img src="https://travis-ci.org/ArpNetworking/maven-javassist.png"
         alt="Travis Build">
</a>
<a href="http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.arpnetworking.commons%22%20a%3A%22maven-javassist-core%22">
    <img src="https://img.shields.io/maven-central/v/com.arpnetworking.commons/maven-javassist-core.svg"
         alt="Maven Artifact">
</a>

Provides a Maven plugin for processing classes using [Javassist](http://www.javassist.org/) by specifying transformations using the included library.

Usage
-----


Development
-----------

To build the service locally you must satisfy these prerequisites:
* [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

Next, fork the repository, clone and build:

Building:

    maven-javassist> ./mvnw verify

To use the local version in your project you must first install it locally:

    maven-javassist> ./mvnw install

You can determine the version of the local build from the pom.xml file.  Using the local version is intended only for testing or development.

You may also need to add the local repository to your build in order to pick-up the local version:

* Maven - Included by default.
* Gradle - Add *mavenLocal()* to *build.gradle* in the *repositories* block.
* SBT - Add *resolvers += Resolver.mavenLocal* into *project/plugins.sbt*.

License
-------

Published under Apache Software License 2.0, see LICENSE

&copy; Inscope Metrics Inc., 2016
