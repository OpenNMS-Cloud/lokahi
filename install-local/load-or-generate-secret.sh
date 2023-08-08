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
keyFile="$1"; shift
crtFile="$1"; shift

directory=$(dirname $keyFile)
mkdir -p $directory

if [ ! -f $keyFile ]; then
  rm -f $crtFile || true
  openssl genrsa -out $keyFile
  openssl req -new -x509 -days 14 -key $keyFile -subj "/CN=$domain/O=Test/C=US" -out $crtFile
fi

# see if the secret already exists
if [ -n "$(kubectl "$@" get --ignore-not-found=true secret "$secret")" ]; then
  if [ $force -gt 0 ]; then
    kubectl "$@" delete "secrets/$secret"
  else
    echo "Secret '$secret' already exists, not adding. Use '-f' option to force recreation." >&2
    exit 0
  fi
fi

# create secret which is tls, by default tls secrets have no "ca.crt" field which is mandatory in case if we wish to use
# given secret at ingress for mtls. We append ca.crt in a patch call to keep secret as a tls, but also include ca.crt
kubectl "$@" create secret tls "$secret" --key=$keyFile \
  --cert=$crtFile || (echo "Could not create $secret" && exit 1)
caContents=$(kubectl "$@" get secret "$secret" -o jsonpath="{.data['tls\.crt']}")
kubectl "$@" patch secret "$secret" -p "{\"data\":{\"ca.crt\":\"${caContents}\"}}" \
  || (echo "Could not supply 'ca.crt' field under secret $secret" && exit 2)
