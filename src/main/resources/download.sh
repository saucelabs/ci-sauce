#!/bin/sh
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
URL=${URL:=https://saucelabs.com/versions.json}

rm sc-*.tar.gz sc-*.zip
curl -s $URL | jq '.["Sauce Connect"] | values[] | select (type=="object") | .download_url' | xargs wget
CURRENT_SC_VERSION=$(curl -s $URL | jq '.["Sauce Connect"].version')
perl -pi -e "s/String CURRENT_SC_VERSION = \"(\d+\.?)+\"/String CURRENT_SC_VERSION = $CURRENT_SC_VERSION/" $DIR/../java/com/saucelabs/ci/sauceconnect/SauceConnectFourManager.java
