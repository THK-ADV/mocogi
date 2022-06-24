#!/bin/sh

img_name=mocogi-backend
dock_hub_URL=dockhub.gm.fh-koeln.de
dock_hub_username=dobrynin
dock_hub_img_location=${dock_hub_URL}/${dock_hub_username}/${img_name}

buildDockerImage() {
  docker image rm ${img_name}
  docker build -t ${img_name} .
}

clearDockerImages() {
  docker-compose stop &&
  docker-compose down &&
  docker image rm ${img_name}
  docker image prune -f
}

uploadDockHub() {
  docker login ${dock_hub_URL} &&
  docker tag ${img_name} ${dock_hub_img_location} &&
  docker push ${dock_hub_img_location} &&
  echo "successfully uploaded image ${img_name} to ${dock_hub_URL}"
}

case "$1" in
"local")
  clearDockerImages &&
    buildDockerImage &&
    docker-compose up -d &&
     exit 0
  ;;
"dockHub")
  clearDockerImages &&
    buildDockerImage &&
    uploadDockHub &&
     exit 0
  ;;
*)
  echo expected local or dockHub, but was $1
  exit 1
  ;;
esac