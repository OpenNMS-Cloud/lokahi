#!/bin/bash

set -euo pipefail

namespace="$1"
domain="$2"

secret="$3"
caKeyFile="$4"
caCrtFile="$5"

if [ ! -f $caKeyFile ]; then
  echo "Required file $caKeyFile does not exist!"
  exit 1
fi

directory=$(dirname $caKeyFile)
id=$(uuidgen)

openssl req -newkey rsa:2048 -nodes -keyout "$directory/$id.key" -subj "/CN=$domain/O=Test/C=US" -out "$directory/$id.csr"
openssl x509 -req -extfile <(printf "subjectAltName=DNS:$domain") -days 14 -in  "$directory/$id.csr" \
  -CA $caCrtFile -CAkey $caKeyFile -CAcreateserial -out "$directory/$id.crt" || (echo "Certificate $id signing failed" && exit 1)
kubectl -n $namespace delete "secrets/$secret" || true
kubectl -n $namespace create secret tls $secret --cert "$directory/$id.crt" --key "$directory/$id.key"

rm "$directory/$id.crt" "$directory/$id.key" "$directory/$id.csr"
