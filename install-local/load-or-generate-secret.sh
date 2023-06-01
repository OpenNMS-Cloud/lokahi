#!/bin/bash

set -euo pipefail
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; exit $s' ERR

if [ $# -ge 1 ] && [ "$1" == "-f" ]; then
    force=1
    shift
else
    force=0
fi

namespace="$1"
domain="$2"

secret="$3"
keyFile="$4"
crtFile="$5"

directory=$(dirname $keyFile)
mkdir -p $directory

if [ ! -f $keyFile ]; then
  rm -f $crtFile || true
  openssl genrsa -out $keyFile
  openssl req -new -x509 -days 14 -key $keyFile -subj "/CN=$domain/O=Test/C=US" -out $crtFile
fi

# see if the secret already exists
if [ -n "$(kubectl -n "$namespace" get --ignore-not-found=true secret "$secret")" ]; then
  if [ $force -gt 0 ]; then
    kubectl -n "$namespace" delete "secrets/$secret"
  else
    echo "Secret '$secret' already exists, not adding. Use '-f' option to force recreation." >&2
    exit 0
  fi
fi

# create secret which is tls, by default tls secrets have no "ca.crt" field which is mandatory in case if we wish to use
# given secret at ingress for mtls. We append ca.crt in a patch call to keep secret as a tls, but also include ca.crt
kubectl -n "$namespace" create secret tls "$secret" --key=$keyFile \
  --cert=$crtFile || (echo "Could not create $secret in namespace $namespace" && exit 1)
caContents=$(kubectl -n "$namespace" get secret "$secret" -o jsonpath="{.data['tls\.crt']}")
kubectl -n "$namespace" patch secret "$secret" -p "{\"data\":{\"ca.crt\":\"${caContents}\"}}" \
  || (echo "Could not supply 'ca.crt' field under secret $secret in namespace $namespace" && exit 2)
