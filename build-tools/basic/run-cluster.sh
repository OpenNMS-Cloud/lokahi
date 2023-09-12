#!/bin/sh

helm upgrade -i lokahi ./charts/lokahi -f build-tools/basic/helm-values.yaml \
  --set OpenNMS.global.image.tag=local-basic
