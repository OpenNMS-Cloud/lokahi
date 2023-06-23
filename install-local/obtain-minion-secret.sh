#!/usr/bin/env bash

# This script will reach specific endpoint to create location and obtain secrets

set -euo pipefail
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; exit $s' ERR

# expected configuration arguments
keycloak="${1:-https://onmshs.local:1443/auth}"
graphql="${2:-https://onmshs.local:1443}"
pkcs12=$(realpath ${3:-target/minion.p12})

# first arguments passed to script are reserved, anything beyond first is passed directly to curl
if [ $# -gt 3 ]; then
  curlArgs=${@:4}
 else
  curlArgs="-k -sSf"
fi

token=$(curl $curlArgs -X POST -H "Content-Type\\: application/x-www-form-urlencoded" \
  -d 'username=admin' \
  -d 'password=admin' \
  -d 'grant_type=password' \
  -d 'scope=openid' \
  -d 'client_id=lokahi' \
  "${keycloak}/realms/opennms/protocol/openid-connect/token" | cut -d "\"" -f 4)

locationName=$(date +'%s')
query="{\"query\": \"mutation { createLocation(location: {location: \\\"LOC_NAME\\\"}) { id, location } }\"}"
query=${query/LOC_NAME/$locationName}

locationId=$(curl $curlArgs -H "Content-Type: application/json" \
  -H "Authorization: Bearer $token" \
  --data "${query}" \
  "${graphql}/api/graphql" | cut -d ":" -f 4|cut -d , -f 1)

query="{\"query\": \"query { getMinionCertificate(location: \\\"LOC_ID\\\") { certificate, password } }\"}"
query=${query/LOC_ID/$locationId}
certificateData=$(curl $curlArgs -H "Content-Type: application/json" \
  -H "Authorization: Bearer $token" \
  --data "${query}" \
  "${graphql}/api/graphql")

# store certificate in given location
echo $certificateData|cut -d "\"" -f 8 | base64 -d > $pkcs12
# emit password to stdout
password=$(echo $certificateData|cut -d "\"" -f 12 | base64)
echo -n "${password}"
