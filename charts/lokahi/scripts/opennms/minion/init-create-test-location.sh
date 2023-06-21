#!/usr/bin/env bash

set -euo pipefail
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; exit $s' ERR

chillax=10
timeout=300

deadline=$(expr $(date +%s) + $timeout)
while [ $(date +%s) -lt $deadline ]; do
  if ! locationId=$(bash /scripts/ensure-location-exists.sh TestLocation https://$INGRESS_HOST_PORT \
        --connect-to $INGRESS_HOST_PORT:ingress-nginx-controller:443); then
    echo "Well, that's not good; let's chillax for a little bit (${chillax}s) and see if things get better" >&2
    sleep $chillax
    continue
  else
    break
  fi
done

openssl genrsa -out /cert/client.key.pkcs1 2048
openssl pkcs8 -topk8 -in /cert/client.key.pkcs1 -out /cert/client.key -nocrypt
openssl req -new -key /cert/client.key -out /cert/client.unsigned.cert -subj "/C=CA/ST=TBD/L=TBD/O=OpenNMS/CN=local-minion/OU=L:${locationId}/OU=T:opennms-prime"
openssl x509 -req -in /cert/client.unsigned.cert -days 14 -CA /run/secrets/mtls/tls.crt -CAkey /run/secrets/mtls/tls.key -out /cert/client.signed.cert
openssl pkcs12 -export -out "/cert/minion.p12" -inkey /cert/client.key -in /cert/client.signed.cert -passout "pass:changeme"
