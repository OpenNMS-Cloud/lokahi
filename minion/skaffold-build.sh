#!/usr/bin/env bash

IMAGE_TAG="local"

if [ -n "${IMAGE}" ]; then
  IMAGE_TAG=$(echo "${IMAGE}" | cut -d : -f 2)
  echo "Applying custom image tag ${IMAGE_TAG}."
fi

mvn install -Ddocker.image.tag=${IMAGE_TAG} -Ddocker.image.skipPush=${!PUSH_IMAGE}
