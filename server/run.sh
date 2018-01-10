#!/usr/bin/env bash
./node_modules/.bin/tsc -w -p . &
node run.js &
./node_modules/.bin/nodemon --watch dist dist/server.js