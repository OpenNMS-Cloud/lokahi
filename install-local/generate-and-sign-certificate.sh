#!/usr/bin/env bash

set -euo pipefail
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; exit $s' ERR

if [ $# -ge 1 ] && [ "$1" == "-f" ]; then
    force=1
    shift
else
    force=0
fi

if [ $# -lt 4 ]; then
    echo "$(basename "$0"): too few command-line arguments" >&2
    echo "usage: $(basename "$0"): [-f] <domain> <secret> <keyFile> <crtFile> [<kubectl options>]" >&2
    exit 1
fi

domain="$1"; shift
secret="$1"; shift
caKeyFile="$1"; shift
caCrtFile="$1"; shift

if [ ! -f $caKeyFile ]; then
  echo "Required file $caKeyFile does not exist!"
  exit 1
fi

directory=$(dirname $caKeyFile)
id=$(uuidgen)

# see if the secret already exists
if [ -n "$(kubectl "$@" get --ignore-not-found=true secret "$secret")" ]; then
  if [ $force -gt 0 ]; then
    kubectl "$@" delete "secrets/$secret"
  else
    echo "Secret '$secret' already exists, not adding. Use '-f' option to force recreation." >&2
    exit 0
  fi
fi

openssl req -newkey rsa:2048 -nodes -keyout "$directory/$id.key" -subj "/CN=$domain/O=Test/C=US" -out "$directory/$id.csr"
openssl x509 -req -extfile <(printf "subjectAltName=DNS:$domain") -days 14 -in  "$directory/$id.csr" \
  -CA $caCrtFile -CAkey $caKeyFile -CAcreateserial -out "$directory/$id.crt" || (echo "Certificate $id signing failed" && exit 1)
kubectl "$@" create secret tls $secret --cert "$directory/$id.crt" --key "$directory/$id.key"

rm "$directory/$id.crt" "$directory/$id.key" "$directory/$id.csr"
