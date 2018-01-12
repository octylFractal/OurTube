#!/usr/bin/env bash
echo 'Building server...'; ./server/build.sh
cd client
echo 'Building client...'; node ./bundle.js
