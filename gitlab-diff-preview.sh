#!/bin/bash

# $1 = main branch
# $2 = draft branch
git switch $1 >/dev/null 2>&1 &&
git pull >/dev/null 2>&1 &&
git switch $2 >/dev/null 2>&1 &&
git pull origin $2 >/dev/null 2>&1 &&
git diff --name-status $1