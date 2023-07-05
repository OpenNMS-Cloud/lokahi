#!/usr/bin/env bash

# https://github.com/olivergondza/bash-strict-mode
set -eEuo pipefail
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; exit $s' ERR

chillax=10
timeout=300

deadline=$(expr $(date +%s) + $timeout)
while true; do
  if [ $(date +%s) -gt $deadline ]; then
    echo "Didn't setup location and get certificate within ${timeout}s" >&2
    exit 1
  fi

  if bash /scripts/prepareLocationAndCerts.sh \
              -U https://$INGRESS_HOST_PORT \
              -k -c "--connect-to" -c "$INGRESS_HOST_PORT:ingress-nginx-controller:443" \
              -f "/cert/minion.p12" -P changeme \
              -l default; then
    break
  fi

  echo "Well, that's not good; let's chillax for a little bit (${chillax}s) and see if things get better" >&2
  sleep $chillax
done
