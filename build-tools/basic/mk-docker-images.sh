#!/bin/bash
########################################################################################################################
##
## opennms/grafana-dev
## opennms/horizon-stream-api
## opennms/horizon-stream-core
## opennms/horizon-stream-keycloak-dev
## opennms/horizon-stream-minion
## opennms/horizon-stream-minion-gateway
## opennms/horizon-stream-notification
## opennms/horizon-stream-ui
##
########################################################################################################################

set -e

time {
	echo ""
	echo "==="
	echo "=== PLATFORM IMAGE"
	echo "==="
	mvn -f platform/assemblies/docker install -Pbuild-docker-images-enabled -DskipTests -Ddocker.image=opennms/horizon-stream-core:local-basic -Ddocker.skipPush=true

	echo ""
	echo "==="
	echo "=== MINION IMAGE"
	echo "==="
	mvn -f minion/docker-assembly install -Pbuild-docker-images-enabled -DskipTests -Ddocker.image=opennms/horizon-stream-minion:local-basic -Ddocker.skipPush=true

	echo ""
	echo "==="
	echo "=== MINION-GATEWAY IMAGE"
	echo "==="
	mvn -f minion-gateway/main jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dimage=opennms/horizon-stream-minion-gateway:local-basic

	echo ""
	echo "=== REST-SERVER (AKA API) IMAGE"
	echo "==="
	echo "==="
	mvn -f rest-server jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dimage=opennms/horizon-stream-rest-server:local-basic

	echo ""
	echo "==="
	echo "=== INVENTORY IMAGE"
	echo "==="
	mvn -f inventory jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dimage=opennms/horizon-stream-inventory:local-basic

	echo ""
	echo "==="
	echo "=== NOTIFICATION IMAGE"
	echo "==="
	mvn -f notifications jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dimage=opennms/horizon-stream-notification:local-basic

	echo ""
	echo "==="
	echo "=== METRICS PROCESSOR IMAGE"
	echo "==="
	mvn -f metrics-processor jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dimage=opennms/horizon-stream-metrics-processor:local-basic

	echo ""
	echo "==="
	echo "=== KEYCLOAK UI IMAGE"
	echo "==="
	DOCKER_BUILDKIT=1 docker build \
		--target development \
		-t opennms/horizon-stream-keycloak-dev:local-basic \
		keycloak-ui

	echo ""
	echo "==="
	echo "=== GRAFANA IMAGE"
	echo "==="
	docker build -t opennms/horizon-stream-grafana-dev:local-basic grafana

	echo ""
	echo "==="
	echo "=== HORIZON UI IMAGE"
	echo "==="
	DOCKER_BUILDKIT=1 docker build \
		--build-arg VITE_BASE_URL=http://localhost:14080 \
		--build-arg VITE_KEYCLOAK_URL=http://localhost:26080 \
		--target development \
		-t opennms/horizon-stream-ui:local-basic \
		ui
}
