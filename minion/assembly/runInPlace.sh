#!/usr/bin/env bash
set -e 

# defaults
MINION_ID='minion-standalone'
MINION_LOCATION='minion-standalone-loc'
IGNITE_SERVER_ADDRESSES=localhost
MINION_GATEWAY_HOST=localhost
MINION_GATEWAY_PORT=8990
MINION_GATEWAY_TLS=false

USAGE() { echo -e "Usage: bash $0 [-i <MINION_ID>] [-l <MINION_LOCATION>] [-h <MINION_GATEWAY_HOST>] [-p <MINION_GATEWAY_PORT>] [-a <IGNITE_SERVER_ADDRESSES>] [-d] [-t]\n\t-d: enable jvm debug\n\t-t: MINION_GATEWAY_TLS" 1>&2; exit 1; }
CHECK_GETOPTS() {
  if [ $? != 0 ]; then
    USAGE
  fi
}

while getopts i:l:h:p:a:dt FLAG 
do
    case "${FLAG}" in
        i) CHECK_GETOPTS ; MINION_ID=${OPTARG} ;;
        l) CHECK_GETOPTS ; MINION_LOCATION=${OPTARG} ;;
        h) CHECK_GETOPTS ; MINION_GATEWAY_HOST=${OPTARG} ;;
        p) CHECK_GETOPTS ; MINION_GATEWAY_PORT=${OPTARG} ;;
        a) CHECK_GETOPTS ; IGNITE_SERVER_ADDRESSES=${OPTARG} ;;
        t) MINION_GATEWAY_TLS=true;;
        d) DEBUG=debug;;
        ?) USAGE ;;
    esac
done

CURRENT_PATH=$(pwd)
SCRIPT_DIR=$( dirname -- "$0" )
cd $SCRIPT_DIR/target/assembly/bin
env MINION_ID="${MINION_ID}" MINION_LOCATION="${MINION_LOCATION}" USE_KUBERNETES=false \
  IGNITE_SERVER_ADDRESSES=${IGNITE_SERVER_ADDRESSES} MINION_GATEWAY_HOST=${MINION_GATEWAY_HOST} \
  MINION_GATEWAY_PORT=${MINION_GATEWAY_PORT} MINION_GATEWAY_TLS="${MINION_GATEWAY_TLS}" \
  ./karaf $DEBUG
cd $CURRENT_PATH
