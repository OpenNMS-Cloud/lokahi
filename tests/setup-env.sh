#!/usr/bin/env bash

# https://github.com/olivergondza/bash-strict-mode
set -eEuo pipefail
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; exit $s' ERR

password=$(uuidgen)
bash ../charts/lokahi/scripts/opennms/minion/prepareLocationAndCerts.sh \
	-U https://onmshs.local:1443 \
	-k \
	-f minion.p12 -P "${password}"
echo "Created minion certificate in minion.p12"

image=$(docker image ls | grep lokahi-minion | grep tilt-build | head -1 | awk '{ print $1 ":" $2 }')

rm -f .env
echo "IMAGE=${image}" >> .env
echo "PASSWORD=${password}" >> .env

echo "Updated .env with ${image} image and minion.p12 password"
