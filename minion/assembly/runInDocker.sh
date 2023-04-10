#!/bin/sh

set -e

GATEWAY_HOST="$(hostname)"

exec docker run \
                --rm \
                -it \
                -e GRPC_CERT_OVERRIDE_AUTHORITY=opennms-minion-ssl-gateway \
                -e MINION_GATEWAY_HOST="${GATEWAY_HOST}" \
                -e MINION_GATEWAY_PORT="9443" \
                -e MINION_ID="MINION-STANDALONE-DOCKER" \
                -e MINION_LOCATION="DOCKER-STANDALONE-LOC" \
                -e MINION_GATEWAY_TLS="true" \
                -e USE_KUBERNETES="false" \
                -e CERT_PKG_PASSWORD=passw0rd \
                --mount "type=bind,source=$(pwd)/../../tools/SSL/minion-cert.insecure.zip,target=/opt/karaf/certs.in/minion-cert.zip" \
                opennms/horizon-stream-minion:latest \
		console
