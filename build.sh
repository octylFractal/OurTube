#!/usr/bin/env bash
pushd server
echo 'server: npm install'; npm i
echo 'server: build'; ./build.sh
popd
pushd client
echo 'client: npm install'; npm i
echo 'client: build'; node ./bundle.js
