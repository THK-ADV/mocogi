#!/bin/bash

# $1 $GIT_EMAIL
# $2 $GIT_USERNAME
# $3 $GIT_HOST
# $4 $GIT_ACCESS_TOKEN
# $5 $GIT_REPO
git config --global pull.rebase false &&
git config --global user.email $1 &&
git config --global user.name $$2 &&
glab config set -g editor vim &&
glab config set -g host $3 &&
glab config set -g token $4 &&
glab auth login -h $3 -t $4 &&
glab repo clone https://oauth2:$4@$3/$5.git