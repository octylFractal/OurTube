#!/usr/bin/env bash
echo 'npm install'; npm i
echo 'Building server...'; ./server/build.sh
cd client
echo 'Building client...'; node ./bundle.js
