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
	echo "=== MINION IMAGE"
	echo "==="
	mvn -f minion/docker-assembly install -Pbuild-docker-images-enabled -DskipTests -Dapplication.docker.image=opennms/horizon-stream-minion:local-basic -Ddocker.skipPush=true

	echo ""
	echo "==="
	echo "=== MINION-GATEWAY IMAGE"
	echo "==="
	mvn -f minion-gateway/main jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/horizon-stream-minion-gateway:local-basic

	echo ""
	echo "==="
	echo "=== MINION-GATEWAY-GRPC-PROXY IMAGE"
	echo "==="
	mvn -f minion-gateway-grpc-proxy/main jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/horizon-stream-minion-gateway-grpc-proxy:local-basic

	echo ""
	echo "=== REST-SERVER (AKA API) IMAGE"
	echo "==="
	echo "==="
	mvn -f rest-server jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/horizon-stream-rest-server:local-basic

	echo ""
	echo "==="
	echo "=== INVENTORY IMAGE"
	echo "==="
	mvn -f inventory jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/horizon-stream-inventory:local-basic

		echo ""
  	echo "==="
  	echo "=== ALARM SERVICE IMAGE"
  	echo "==="
  	mvn -f alarm jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/horizon-stream-alarm:local-basic

	echo ""
	echo "==="
	echo "=== NOTIFICATION IMAGE"
	echo "==="
	mvn -f notifications jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/horizon-stream-notification:local-basic

	echo ""
	echo "==="
	echo "=== METRICS PROCESSOR IMAGE"
	echo "==="
	mvn -f metrics-processor jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/horizon-stream-metrics-processor:local-basic

  echo ""
  echo "==="
  echo "=== EVENTS IMAGE"
  echo "==="
  mvn -f events jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/horizon-stream-events:local-basic

  echo ""
  echo "==="
  echo "=== DATACHOICES IMAGE"
  echo "==="
  mvn -f events jib:dockerBuild -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/horizon-stream-datachoices:local-basic

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
