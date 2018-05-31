#!/usr/bin/env bash
set -e
pushd serverj
echo 'server: gradle build'; ./gradlew clean && ./gradlew build
echo 'server: unpack archive to build/latest'; unar -o build/latest/ build/distributions/*.zip
popd
pushd client
echo 'client: npm install'; npm i
echo 'client: build'; node ./bundle.js
