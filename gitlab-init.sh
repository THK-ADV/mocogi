#!/bin/bash

# $1 GIT_EMAIL
# $2 GIT_USERNAME
# $3 GIT_HOST
# $4 GIT_ACCESS_TOKEN
# $5 GIT_REPO_PATH
# $6 GIT_REPO_NAME
glab auth login -h $3 -t $4 &&
glab repo clone https://oauth2:$4@$3/$5.git &&
cd $6 &&
git config pull.rebase false &&
git config user.email $1 &&
git config user.name $2 &&
glab config set pull.rebase false &&
glab config set user.email $1 &&
glab config set user.name $2 &&
glab config set editor vim &&
glab config set host $3 &&
glab config set token $4