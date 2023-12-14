#!/bin/bash

# $1 = draft branch
echo ">> switch to $1" &&
git switch $1 &&
echo ">> pulling" &&
git pull origin $1 &&
echo ">> done!"