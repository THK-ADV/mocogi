#!/bin/sh

backend_img_name=mocogi-backend
frontend_img_name=mocogi-frontend
frontend_path=/Users/alex/Developer/mocogi-ui
dock_hub_URL=dockhub.gm.fh-koeln.de
dock_hub_username=dobrynin
dock_hub_img_location=${dock_hub_URL}/${dock_hub_username}/${backend_img_name}

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

clearDockerImages() {
  docker image rm $1
  docker image prune -f
}

uploadDockHub() {
  docker login ${dock_hub_URL} &&
    docker tag ${backend_img_name} ${dock_hub_img_location} &&
    docker push ${dock_hub_img_location} &&
    echo "successfully uploaded image ${backend_img_name} to ${dock_hub_URL}"
}

case "$1" in
"backend")
  stop &&
    clearDockerImages $backend_img_name &&
    buildBackend &&
    docker-compose up -d &&
    exit 0
  ;;
"dockhub")
  stop &&
    clearDockerImages $backend_img_name &&
    buildBackend &&
    uploadDockHub &&
    exit 0
  ;;
"frontend")
  stop &&
    clearDockerImages $frontend_img_name &&
    buildFrontend &&
    docker run -d -p 8080:80 --name $frontend_img_name $frontend_img_name
  exit 0
  ;;

"both")
  stop &&
    clearDockerImages $backend_img_name &&
    clearDockerImages $frontend_img_name &&
    buildBackend &&
    buildFrontend &&
    docker-compose up -d &&
    exit 0
  ;;
*)
  echo expected backend, frontend, both or dockhub, but was $1
  exit 1
  ;;
esac
