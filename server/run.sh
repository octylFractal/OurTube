#!/usr/bin/env bash
cd "$(dirname "$(readlink -f "$0")")"

export PATH="$PATH:$(realpath ./node_modules/.bin/)"
echo "Using path $PATH"
tsc -w -p . &
node run.js &
nodemon --watch dist dist/server.js