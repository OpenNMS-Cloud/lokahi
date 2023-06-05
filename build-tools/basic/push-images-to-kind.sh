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
kind load docker-image "opennms/lokahi-grafana:local-basic"
kind load docker-image "opennms/lokahi-minion:local-basic"
kind load docker-image "opennms/lokahi-ui:local-basic"
kind load docker-image "opennms/lokahi-keycloak:local-basic"
kind load docker-image "opennms/lokahi-inventory:local-basic"
kind load docker-image "opennms/lokahi-alert:local-basic"
kind load docker-image "opennms/lokahi-notification:local-basic"
kind load docker-image "opennms/lokahi-rest-server:local-basic"
kind load docker-image "opennms/lokahi-minion-gateway:local-basic"
kind load docker-image "opennms/lokahi-minion-certificate-manager:local-basic"
kind load docker-image "opennms/lokahi-minion-certificate-verifier:local-basic"
kind load docker-image "opennms/lokahi-metrics-processor:local-basic"
kind load docker-image "opennms/lokahi-events:local-basic"
kind load docker-image "opennms/lokahi-datachoices:local-basic"

###
### FASTER WAY TO LOAD - HOWEVER, for some reason this is not loading all of the images, and tags are wrong
###
# (
# 	docker save \
# 		"opennms/lokahi-alert:local-basic" \
# 		"opennms/lokahi-datachoices:local-basic" \
# 		"opennms/lokahi-events:local-basic" \
# 		"opennms/lokahi-grafana:local-basic" \
# 		"opennms/lokahi-inventory:local-basic" \
# 		"opennms/lokahi-keycloak:local-basic" \
# 		"opennms/lokahi-metrics-processor:local-basic" \
# 		"opennms/lokahi-minion-certificate-manager:local-basic" \
# 		"opennms/lokahi-minion-certificate-verifier:local-basic" \
# 		"opennms/lokahi-minion-gateway:local-basic" \
# 		"opennms/lokahi-minion:local-basic" \
# 		"opennms/lokahi-notification:local-basic" \
# 		"opennms/lokahi-rest-server:local-basic" \
# 		"opennms/lokahi-ui:local-basic"
# ) | \
# (
# 	docker exec -i "kind-control-plane" ctr images import --snapshotter overlayfs -
# )
