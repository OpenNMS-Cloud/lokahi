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
# 
# kind load docker-image docker.io/bitnami/kafka:3 \
# 	docker.io/bitnami/zookeeper:3.7 \
# 	mailhog/mailhog:latest \
# 	nginx:1.21.6-alpine \
# 	opennms/minion:29.0.10 \
# 	postgres:13.3-alpine \
# 	busybox \
# 	"opennms/horizon-stream-grafana-dev:local-basic" \
# 	"opennms/horizon-stream-core:local-basic" \
# 	"opennms/horizon-stream-minion:local-basic" \
# 	"opennms/horizon-stream-ui:local-basic" \
# 	"opennms/horizon-stream-keycloak-dev:local-basic" \
# 	"opennms/horizon-stream-inventory:local-basic" \
# 	"opennms/horizon-stream-alarm:local-basic" \
# 	"opennms/horizon-stream-notification:local-basic" \
# 	"opennms/horizon-stream-rest-server:local-basic" \
# 	"opennms/horizon-stream-minion-gateway:local-basic" \
# 	"opennms/horizon-stream-minion-gateway-grpc-proxy:local-basic" \
# 	"opennms/horizon-stream-metrics-processor:local-basic" \
# 	"opennms/horizon-stream-events:local-basic" \
# 	"opennms/horizon-stream-datachoices:local-basic"

(
	docker save \
		"opennms/horizon-stream-alarm:local-basic" \
		"opennms/horizon-stream-datachoices:local-basic" \
		"opennms/horizon-stream-events:local-basic" \
		"opennms/horizon-stream-grafana:local-basic" \
		"opennms/horizon-stream-inventory:local-basic" \
		"opennms/horizon-stream-keycloak:local-basic" \
		"opennms/horizon-stream-metrics-processor:local-basic" \
		"opennms/horizon-stream-minion-gateway-grpc-proxy:local-basic" \
		"opennms/horizon-stream-minion-gateway:local-basic" \
		"opennms/horizon-stream-minion:local-basic" \
		"opennms/horizon-stream-notification:local-basic" \
		"opennms/horizon-stream-rest-server:local-basic" \
		"opennms/horizon-stream-ui:local-basic"
) | \
(
	docker exec -i "kind-control-plane" ctr images import --snapshotter overlayfs -
)

