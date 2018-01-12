#!/usr/bin/env bash
cd "$(dirname "$(readlink -f "$0")")"
./node_modules/.bin/tsc -w -p . &
node run.js &
./node_modules/.bin/nodemon --watch dist dist/server.js