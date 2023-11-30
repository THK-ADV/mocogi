#!/bin/sh

backend_img_name=mocogi-backend
frontend_img_name=mocogi-frontend
frontend_path=/Users/alex/Developer/mocogi-ui

buildBackend() {
  docker build -t ${backend_img_name} .
}

buildFrontend() {
  docker build -t $frontend_img_name $frontend_path
}

stop() {
  docker-compose stop &&
    docker-compose down
}

clearDockerImage() {
  docker image rm $1
}

clearDocker() {
  docker image prune -f
  docker container prune -f
}

case "$1" in
"core")
  img_name=mocogi-core:latest
  docker build -t ${img_name} -f Dockerfile_Dependencies . &&
  exit 0
  ;;
"backend_build")
  buildBackend &&
    exit 0
  ;;
"backend")
  stop &&
    clearDockerImage $backend_img_name
    clearDocker
    buildBackend &&
    docker-compose up -d &&
    exit 0
  ;;
"frontend")
  stop &&
    clearDockerImage $frontend_img_name
    clearDocker
    buildFrontend &&
    docker run -d -p 8080:80 --name $frontend_img_name $frontend_img_name
  exit 0
  ;;

"both")
  stop &&
    clearDockerImage $backend_img_name
    clearDockerImage $frontend_img_name
    clearDocker
    buildBackend &&
    buildFrontend &&
    docker-compose up -d &&
    exit 0
  ;;
*)
  echo expected core, backend, frontend or both, but was $1
  exit 1
  ;;
esac
