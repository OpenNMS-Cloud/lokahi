#!/bin/bash
########################################################################################################################
##
## opennms/grafana
## opennms/lokahi-api
## opennms/lokahi-keycloak
## opennms/lokahi-minion
## opennms/lokahi-minion-gateway
## opennms/lokahi-notification
## opennms/lokahi-ui
##
########################################################################################################################

set -e

time {
	echo ""
	echo "==="
	echo "=== MINION IMAGE"
	echo "==="
	mvn -f minion/docker-assembly install -Pbuild-docker-images-enabled -DskipTests -Dapplication.docker.image=opennms/lokahi-minion:local-basic -Ddocker.skipPush=true "$@"

	echo ""
	echo "==="
	echo "=== MINION-GATEWAY IMAGE"
	echo "==="
	mvn -f minion-gateway/main install -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/lokahi-minion-gateway:local-basic "$@"

	echo ""
	echo "==="
	echo "=== MINION-CERTIFICATE-MANAGER IMAGE"
	echo "==="
	mvn -f minion-certificate-manager/main install -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/lokahi-minion-certificate-manager:local-basic "$@"

	echo ""
	echo "==="
	echo "=== MINION-CERTIFICATE-VERIFIER IMAGE"
	echo "==="
	mvn -f minion-certificate-verifier/main install -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/lokahi-minion-certificate-verifier:local-basic "$@"

	echo ""
	echo "=== REST-SERVER (AKA API) IMAGE"
	echo "==="
	echo "==="
	mvn -f rest-server install -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/lokahi-rest-server:local-basic "$@"

	echo ""
	echo "==="
	echo "=== INVENTORY IMAGE"
	echo "==="
	mvn -f inventory install -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/lokahi-inventory:local-basic "$@"

	echo ""
	echo "==="
	echo "=== ALERT SERVICE IMAGE"
	echo "==="
	mvn -f alert install -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/lokahi-alert:local-basic "$@"

	echo ""
	echo "==="
	echo "=== NOTIFICATION IMAGE"
	echo "==="
	mvn -f notifications install -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/lokahi-notification:local-basic "$@"

	echo ""
	echo "==="
	echo "=== METRICS PROCESSOR IMAGE"
	echo "==="
	mvn -f metrics-processor install -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/lokahi-metrics-processor:local-basic "$@"

	echo ""
	echo "==="
	echo "=== EVENTS IMAGE"
	echo "==="
	mvn -f events install -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/lokahi-events:local-basic "$@"

	echo ""
	echo "==="
	echo "=== DATACHOICES IMAGE"
	echo "==="
	mvn -f datachoices install -Djib.container.creationTime=USE_CURRENT_TIMESTAMP -Dapplication.docker.image=opennms/lokahi-datachoices:local-basic "$@"

	echo ""
	echo "==="
	echo "=== KEYCLOAK UI IMAGE"
	echo "==="
	DOCKER_BUILDKIT=1 docker build \
		--target development \
		-t opennms/lokahi-keycloak:local-basic \
		keycloak-ui

	echo ""
	echo "==="
	echo "=== GRAFANA IMAGE"
	echo "==="
	docker build \
		-t opennms/lokahi-grafana:local-basic \
		grafana

	echo ""
	echo "==="
	echo "=== LOKAHI UI IMAGE"
	echo "==="
	DOCKER_BUILDKIT=1 docker build \
		--build-arg VITE_BASE_URL=http://localhost:14080 \
		--build-arg VITE_KEYCLOAK_URL=http://localhost:26080 \
		--target production \
		-t opennms/lokahi-ui:local-basic \
		ui
}
