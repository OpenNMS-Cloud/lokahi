#!/usr/bin/env bash

set -euo pipefail
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; exit $s' ERR

url="https://onmshs.local:1443"

keycloak="${url}/auth/"
graphql="${url}/"

token=$(curl "$@" -sSf -k -X POST -H "Content-Type\\: application/x-www-form-urlencoded" \
  -d 'username=admin' \
  -d 'password=admin' \
  -d 'grant_type=password' \
  -d 'scope=openid' \
  -d 'client_id=lokahi' \
  "${keycloak}realms/opennms/protocol/openid-connect/token" | cut -d "\"" -f 4)

locationName=$(date +'%s')
query="{\"query\": \"mutation { createLocation(location: {location: \\\"LOC_NAME\\\"}) { id, location } }\"}"
query=${query/LOC_NAME/$locationName}
locationId=$(curl "$@" -sSf -k -H "Content-Type: application/json" \
  -H "Authorization: Bearer $token" \
  --data "${query}" \
  "${graphql}api/graphql" | cut -d ":" -f 4|cut -d , -f 1)

query="{\"query\": \"query { getMinionCertificate(location: \\\"LOC_ID\\\") { certificate, password } }\"}"
#query=${query/LOC_ID/$locationId}
query=${query/LOC_ID/$locationName}
certificateData=$(curl "$@" -sSf -k -H "Content-Type: application/json" \
  -H "Authorization: Bearer $token" \
  --data "${query}" \
  "${graphql}api/graphql")

echo $certificateData|cut -d "\"" -f 8 | base64 -d > minion.p12
password=$(echo $certificateData|cut -d "\"" -f 12)
echo "${password}"
