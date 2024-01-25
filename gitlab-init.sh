#!/bin/bash

# $1 GIT_EMAIL
# $2 GIT_USERNAME
# $3 GIT_HOST
# $4 GIT_ACCESS_TOKEN
# $5 GIT_REPO_PATH
# $6 GIT_REPO_NAME
glab config set -g pull.rebase false &&
glab config set -g user.email $1 &&
glab config set -g user.name $2 &&
glab config set -g editor vim &&
glab config set -g host $3 &&
glab config set -g token $4
glab auth login -h $3 -t $4 &&
glab repo clone https://oauth2:$4@$3/$5.git