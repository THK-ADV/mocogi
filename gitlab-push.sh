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

# $1 = main branch
# $2 = branch name
# $3 = commit msg
if [ -n "$(git status --porcelain)" ]; then
  echo ">> git switch $1"
  git switch $1
  if [ $? -ne 0 ]; then
      reset_to_head $1 $2
      exit 1
  fi
  echo ">> git pull"
  git pull
  if [ $? -ne 0 ]; then
      reset_to_head $1 $2
      exit 2
  fi
  echo ">> git switch -c $2"
  git switch -c $2
  if [ $? -ne 0 ]; then
      reset_to_head $1 $2
      exit 3
  fi
  git add .
  echo ">> git add ."
  if [ $? -ne 0 ]; then
      reset_to_head $1 $2
      exit 4
  fi
  echo ">> git commit -m "$3""
  git commit -m "$3"
  if [ $? -ne 0 ]; then
      reset_to_head $1 $2
      exit 5
  fi
  echo ">> git push origin $2"
  git push origin $2
  if [ $? -ne 0 ]; then
      reset_to_head $1 $2
      exit 6
  fi
  git branch -D $2
  echo ">> done!"
  exit 0
else
  echo ">> no changes"
  exit 7
fi