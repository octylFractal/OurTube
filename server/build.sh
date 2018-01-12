#!/usr/bin/env bash
cd "$(dirname "$(readlink -f "$0")")"
./node_modules/.bin/tsc -p .