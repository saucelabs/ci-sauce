ci-sauce
========

[![Maven Central](https://img.shields.io/maven-central/v/com.saucelabs/ci-sauce.svg)](https://mvnrepository.com/artifact/com.saucelabs/ci-sauce)

This folder contains the common code for the Bamboo and Jenkins Sauce OnDemand plugins.

To build the plugin, you will need [Maven 2](http://maven.apache.org).

The plugin also references the Sauce Connect 2 library, which is hosted in the Sauce Labs Cloudbees Maven repository (https://repository-saucelabs.forge.cloudbees.com/release).  

It can also be built from source and can be obtained from git@github.com:saucelabs/sauce-connect.git.  Once the source has been obtained, it can be built by running 'mvn package' and installed by running:

	mvn install:install-file -DgroupId=com.saucelabs -DartifactId=sauce-connect -Dversion=3.0 -Dpackaging=jar -Dfile=/path/to/sauce-connect.jar

To build (compile,test,jar) the plugin run: 

	mvn package

To build and deploy a new version of the library, you will need to modify your Maven settings.xml file to include the username/password for the Sauce Labs CloudBees instance (https://cloudbees.zendesk.com/entries/421064-maven-guide), then:

	    - Run atlas-mvn release:prepare.  You will be prompted to enter the version to be released, the tag to be applied, and the next version number (ending with SNAPSHOT)
	    - Run atlas-mvn release:perform.  This will upload a copy of the jar file to https://repository-saucelabs.forge.cloudbees.com/release
