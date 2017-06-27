ci-sauce
========

[![Maven Central](https://img.shields.io/maven-central/v/com.saucelabs/ci-sauce.svg)](https://mvnrepository.com/artifact/com.saucelabs/ci-sauce)

This folder contains the common code for the Bamboo and Jenkins Sauce OnDemand plugins.

To update sauce connect, run the download script, src/main/resources/download.sh
This will download all the required files, you just need to update git.
Make sure the jq (https://stedolan.github.io/jq/) app is installed in your path

To build the plugin, you will need [Maven 2](http://maven.apache.org).

To build (compile,test,jar) the plugin run: 

	mvn package
  
To release the plugin to maven central

  mvn release:prepare release:perform
