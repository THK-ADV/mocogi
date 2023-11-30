#!/bin/bash

# $1 = main branch
# $2 = branch name
# $3 = commit msg
# $4 = mr title
if [ -n "$(git status --porcelain)" ]; then
  echo ">> switch to $1" &&
  git switch $1 &&
  echo ">> pulling" &&
  git pull &&
  echo ">> create new branch $2" &&
  git switch -c $2 &&
  echo ">> adding changes" &&
  git add . &&
  echo ">> committing changes" &&
  git commit -m "$3" &&
  echo ">> creating merge request" &&
  glab mr create -b $1 -t "$4" -f -y --remove-source-branch --squash-before-merge &&
  echo ">> merging merge request" &&
  glab mr merge -s -y &&
  echo ">> switch to $1" &&
  git switch $1 &&
  echo ">> pulling" &&
  git pull &&
  echo ">> deleting branch $2" &&
  git branch -d $2
  echo ">> done!"
else
  echo ">> no changes" &&
  exit -1
fi