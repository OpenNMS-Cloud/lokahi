#!/bin/bash

# Please avoid committing tweaks to this script.

docker pull docker.io/bitnami/kafka:3
docker pull docker.io/bitnami/zookeeper:3.7
docker pull mailhog/mailhog:latest
docker pull nginx:1.21.6-alpine
docker pull opennms/minion:29.0.10
docker pull postgres:13.3-alpine
docker pull busybox

# Don't use "kind load" - it is slow
# (see the bug fix for "kind load" multiple images simultaneously - when this is available, using the following kind load instead should be just as fast)
# (bug report here: https://github.com/kubernetes-sigs/kind/issues/3063)

# Reverting to the slow kind load; don't load multiple at once (as per the bug above)

kind load docker-image docker.io/bitnami/kafka:3
kind load docker-image docker.io/bitnami/zookeeper:3.7
kind load docker-image mailhog/mailhog:latest
kind load docker-image nginx:1.21.6-alpine
kind load docker-image opennms/minion:29.0.10
kind load docker-image postgres:13.3-alpine
kind load docker-image busybox
kind load docker-image "opennms/horizon-stream-grafana:local-basic"
kind load docker-image "opennms/horizon-stream-minion:local-basic"
kind load docker-image "opennms/horizon-stream-ui:local-basic"
kind load docker-image "opennms/horizon-stream-keycloak:local-basic"
kind load docker-image "opennms/horizon-stream-inventory:local-basic"
kind load docker-image "opennms/horizon-stream-alert:local-basic"
kind load docker-image "opennms/horizon-stream-notification:local-basic"
kind load docker-image "opennms/horizon-stream-rest-server:local-basic"
kind load docker-image "opennms/horizon-stream-minion-gateway:local-basic"
kind load docker-image "opennms/horizon-stream-minion-certificate-manager:local-basic"
kind load docker-image "opennms/horizon-stream-minion-certificate-verifier:local-basic"
kind load docker-image "opennms/horizon-stream-metrics-processor:local-basic"
kind load docker-image "opennms/horizon-stream-events:local-basic"
kind load docker-image "opennms/horizon-stream-datachoices:local-basic"

###
### FASTER WAY TO LOAD - HOWEVER, for some reason this is not loading all of the images, and tags are wrong
###
# (
# 	docker save \
# 		"opennms/horizon-stream-alert:local-basic" \
# 		"opennms/horizon-stream-datachoices:local-basic" \
# 		"opennms/horizon-stream-events:local-basic" \
# 		"opennms/horizon-stream-grafana:local-basic" \
# 		"opennms/horizon-stream-inventory:local-basic" \
# 		"opennms/horizon-stream-keycloak:local-basic" \
# 		"opennms/horizon-stream-metrics-processor:local-basic" \
# 		"opennms/horizon-stream-minion-certificate-manager:local-basic" \
# 		"opennms/horizon-stream-minion-certificate-verifier:local-basic" \
# 		"opennms/horizon-stream-minion-gateway:local-basic" \
# 		"opennms/horizon-stream-minion:local-basic" \
# 		"opennms/horizon-stream-notification:local-basic" \
# 		"opennms/horizon-stream-rest-server:local-basic" \
# 		"opennms/horizon-stream-ui:local-basic"
# ) | \
# (
# 	docker exec -i "kind-control-plane" ctr images import --snapshotter overlayfs -
# )
