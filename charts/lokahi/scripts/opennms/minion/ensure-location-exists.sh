#!/usr/bin/env bash

set -euo pipefail
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; if [ -f /tmp/curl-headers.txt ]; then grep server-timing: /tmp/curl-headers.txt; fi; exit $s' ERR

if [ $# -lt 2 ]; then
  echo "$(basename $0): too few arguments" >&2
  echo "Usage: $(basename $0) <location name> <base url> [<curl arguments>]" >&2
  exit 1
fi

locationName="$1"; shift
url="$1"; shift

keycloak="${url}/auth/"
graphql="${url}/"

token=$(curl "$@" -sSf -k -X POST -H "Content-Type\\: application/x-www-form-urlencoded" \
  --dump-header /tmp/curl-headers.txt \
  -d 'username=admin' \
  -d 'password=admin' \
  -d 'grant_type=password' \
  -d 'scope=openid' \
  -d 'client_id=lokahi' \
  "${keycloak}realms/opennms/protocol/openid-connect/token" | cut -d "\"" -f 4)

query="{\"query\": \"query { searchLocation(searchTerm: \\\"LOC_NAME\\\") { id, location } }\"}"
query=${query/LOC_NAME/$locationName}
result=$(curl "$@" -sSf -k -H "Content-Type: application/json" \
  --dump-header /tmp/curl-headers.txt \
  -H "Authorization: Bearer $token" \
  --data "${query}" \
  "${graphql}api/graphql")

if grep -q '^{"errors":' <<< "$result"; then
  echo "error getting location: $result" >&2
  exit 1
fi

locationId=$(echo "$result" | cut -d ":" -f 4|cut -d , -f 1)

if [ -n "${locationId}" ]; then
  echo "Found existing location ID ${locationId} for '${locationName}'" >&2
else # if it doesn't exist, create it
  query="{\"query\": \"mutation { createLocation(location: {location: \\\"LOC_NAME\\\"}) { id, location } }\"}"
  query=${query/LOC_NAME/$locationName}
  result=$(curl "$@" -sSf -k -H "Content-Type: application/json" \
    --dump-header /tmp/curl-headers.txt \
    -H "Authorization: Bearer $token" \
    --data "${query}" \
    "${graphql}api/graphql")


  if grep -q '^{"errors":' <<< "$result"; then
    echo "error getting location: $result" >&2
    exit 1
  fi

  locationId=$(echo "$result" | cut -d ":" -f 4|cut -d , -f 1)
  if [ -z "${locationId}" ]; then
    echo "location ID from what appears to be a valid result was empty:" >&2
    cat <<< "$result" >&2
    exit 1
  fi
  echo "Created new location ID ${locationId} for '${locationName}'" >&2
fi
echo "$locationId"
