#!/usr/bin/env bash
set -e
pushd serverj
echo 'server: gradle install'; ./gradlew install
popd
pushd client
echo 'client: npm install'; npm i
echo 'client: build'; node ./bundle.js
