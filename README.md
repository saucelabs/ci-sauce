ci-sauce
========

[![Maven Central](https://img.shields.io/maven-central/v/com.saucelabs/ci-sauce.svg)](https://mvnrepository.com/artifact/com.saucelabs/ci-sauce)

This folder contains the common code for the Bamboo and Jenkins Sauce OnDemand plugins.

To build the plugin, you will need [Maven 2](http://maven.apache.org).

To build (compile,test,jar) the plugin run: 

```
mvn package
```

To release the plugin to maven central

```
mvn release:prepare release:perform
```

http://central.sonatype.org/pages/ossrh-guide.html for more details
