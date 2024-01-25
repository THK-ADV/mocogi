#!/bin/bash

echo_err() {
  echo "$@" 1>&2
}

reset_to_head() {
  echo ">> reset to head"
  git switch $1
  git clean -fd
  git reset --hard
  git branch -D $2
}

close_mr() {
  echo ">> close merge request and delete remote branch"
  glab mr close $2 &&
  git push origin --delete $2 &&
  reset_to_head $1 $2
}

# $1 = main branch
# $2 = branch name
# $3 = commit msg
# $4 = mr title
if [ -n "$(git status --porcelain)" ]; then
  git switch $1 && git pull && git switch -c $2 && git add . && git commit -m "$3"
  if [ $? -ne 0 ]; then
    reset_to_head $1 $2
    exit 1
  fi
  glab mr create -b $1 -t "$4" -f -y --remove-source-branch --squash-before-merge
  if [ $? -ne 0 ]; then
    echo_err ">> creation of merge request failed"
    reset_to_head $1 $2
    exit 2
  fi
  echo ">> waiting 10 seconds before continue..."
  sleep 10 # this is a workaround. merge requests seems to be blocked after creation. wait for 10 seconds before continue with merge
  glab mr merge -s -y
  if [ $? -ne 0 ]; then
      echo_err ">> merging failed"
      close_mr $1 $2
      exit 3
  fi
  git switch $1 && git pull && git branch -d $2
  echo ">> done!"
  exit 0
else
  echo ">> no changes" &&
  exit -1
fi