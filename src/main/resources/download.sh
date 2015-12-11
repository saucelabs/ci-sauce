#!/bin/sh
rm sc-*.tar.gz sc-*.zip
curl https://saucelabs.com/versions.json | jq '.["Sauce Connect"] | values[] | select (type=="object") | .download_url' | xargs wget
