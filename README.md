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
<a href="http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.arpnetworking.commons%22%20a%3A%22javassist-maven-core%22">
    <img src="https://img.shields.io/maven-central/v/com.arpnetworking.commons/javassist-maven-core.svg"
         alt="Maven Artifact">
</a>

Provides a Maven plugin for processing classes using [Javassist](http://www.javassist.org/) by specifying transformations using the included library.

Usage
-----

Add the plugin block to your project. Specify at least the class path to your processor.

```xml
<plugin>
  <groupId>com.arpnetworking.commons</groupId>
  <artifactId>javassist-maven-plugin<artifactId>
  <version>0.1.0</version>
  <executions>
    <execution>
      <id>javassist-process</id>
      <goals>
        <goal>process</goal>
      </goals>
      <configuration>
        <processor>${YOUR_PROCESSOR_CLASS}</processor>
      </configuration>
    </execution>
  </executions>
</plugin>
```

You can also process your test classes by adding an additional execution with the _test-process_ goal:

```xml
<execution>
  <id>javassist-test-process</id>
  <goals>
    <goal>test-process</goal>
  </goals>
  <configuration>
    <processor>${YOUR_PROCESSOR_CLASS}</processor>
  </configuration>
</execution>
```

Additional configuration options include:

* includes - set of path matching globs for including classes for processing; if not specified all classes are included.
* excludes - set of path matching globs for excluding classes from processing; if not specified no classes are excluded.
* threads - the number of threads to execute processing with or threads per core if the value ends with "C".

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
