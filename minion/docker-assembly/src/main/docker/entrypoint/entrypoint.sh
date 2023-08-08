#!/usr/bin/env bash

# https://github.com/olivergondza/bash-strict-mode
set -eEuo pipefail
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; exit $s' ERR

### Required: GRPC_CLIENT_KEYSTORE file encrypted with GRPC_CLIENT_KEYSTORE_PASSWORD

# This needs to match the default in assembly/src/main/resources/etc/org.opennms.core.ipc.grpc.client.cfg
GRPC_CLIENT_KEYSTORE="${GRPC_CLIENT_KEYSTORE:-/opt/karaf/minion.p12}"

if [ ! -e "${GRPC_CLIENT_KEYSTORE}" ]; then
	echo "Required keystore file not found inside the container." >&2
	echo "Please check keystore file on your host system and the '--mount' command passed to docker." >&2
	echo "The keystore file is expected to be mounted at '${GRPC_CLIENT_KEYSTORE}' inside the container." >&2
	echo "Example docker argument to mount keystore file: --mount type=bind,source=<MINION_P12_FILE>,target=${GRPC_CLIENT_KEYSTORE},readonly" >&2
	echo "The text <MINION_P12_FILE> should be replaced with the full path to the minion.p12 file on your host." >&2
	exit 1
fi

if ! [[ -v GRPC_CLIENT_KEYSTORE_PASSWORD ]]; then
	echo "Required keystore password environment variable 'GRPC_CLIENT_KEYSTORE_PASSWORD' not set." >&2
	echo "Example docker argument to pass keystore password: -e GRPC_CLIENT_KEYSTORE_PASSWORD=password" >&2
	exit 1
fi

if ! openssl pkcs12 -legacy -in "${GRPC_CLIENT_KEYSTORE}" -noout -nokeys "${openssl_args[@]}" -passin "pass:${GRPC_CLIENT_KEYSTORE_PASSWORD}"; then
	echo "Could not open keystore using the password provided in GRPC_CLIENT_KEYSTORE_PASSWORD." >&2
	echo "Please check that your keystore file and password are correct." >&2
	exit 1
fi

exec /opt/karaf/bin/karaf "$@"
