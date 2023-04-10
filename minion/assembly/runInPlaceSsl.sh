#!/usr/bin/env bash
set -e 

# defaults
MINION_ID='minion-standalone'
MINION_LOCATION='minion-standalone-loc'
IGNITE_SERVER_ADDRESSES=localhost
MINION_GATEWAY_HOST=localhost
# TODO - port 9443 for TLS?
MINION_GATEWAY_PORT=8990
MINION_GATEWAY_TLS="false"

CERT_ROOTDIR="$(cd ../../tools/SSL; pwd)"
CA_CERT_FILE="${CERT_ROOTDIR}/CA.cert"
CLIENT_KEY_FILE="${CERT_ROOTDIR}/client.key"
CLIENT_CERT_FILE="${CERT_ROOTDIR}/client.signed.cert"



###
### WARNING: certificate passwords currently do not work
### The following error is logged when this fails:
###
###	Caused by: java.security.NoSuchAlgorithmException: PBES2 SecretKeyFactory not available
###
CLIENT_PRIVATE_KEY_PASSWORD=""
# CLIENT_PRIVATE_KEY_PASSWORD="passw0rd"

# Prevent hostname verification failures by setting the expected cert "hostname"
OVERRIDE_AUTHORITY="opennms-minion-ssl-gateway"


USAGE() { echo -e "Usage: bash $0 [-i <MINION_ID>] [-l <MINION_LOCATION>] [-h <MINION_GATEWAY_HOST>] [-p <MINION_GATEWAY_PORT>] [-a <IGNITE_SERVER_ADDRESSES>] [-d] [-t]\n\t-d: enable jvm debug\n\t-t: MINION_GATEWAY_TLS" 1>&2; exit 1; }
CHECK_GETOPTS() {
  if [ $? != 0 ]; then
    USAGE
  fi
}

while getopts a:h:i:l:p:Ddtx FLAG 
do
    case "${FLAG}" in
        a) CHECK_GETOPTS ; IGNITE_SERVER_ADDRESSES=${OPTARG} ;;
        h) CHECK_GETOPTS ; MINION_GATEWAY_HOST=${OPTARG} ;;
        i) CHECK_GETOPTS ; MINION_ID=${OPTARG} ;;
        l) CHECK_GETOPTS ; MINION_LOCATION=${OPTARG} ;;
        p) CHECK_GETOPTS ; MINION_GATEWAY_PORT=${OPTARG} ;;

        D) DEBUG=debugs ;;
        d) DEBUG=debug ;;
        t) MINION_GATEWAY_TLS="true" ;;
        x) MINION_GATEWAY_TLS="false" ;;
        ?) USAGE ;;
    esac
done



###
###
###

SCRIPT_DIR="$( dirname -- "$0" )"
cd "$SCRIPT_DIR"/target/assembly



###
### PREPARE CERTIFICATE CONFIGURATION
###

# Update the config file, if TLS is enabled

GRPC_CLIENT_CONFIG_FILE="etc/org.opennms.core.ipc.grpc.client.cfg"

if [ "${MINION_GATEWAY_TLS}" = "true" ]
then
	(
		cat "${GRPC_CLIENT_CONFIG_FILE}" |
			grep -v '^grpc.trust\.cert\.filepath=' |
			grep -v '^grpc.client\.cert\.filepath=' |
			grep -v '^grpc.client\.private\.key\.filepath=' |
			grep -v '^grpc.client\.private\.key\.password=' |
			grep -v '^grpc.override\.authority='

		echo "grpc.trust.cert.filepath=${CA_CERT_FILE}"
		echo "grpc.client.cert.filepath=${CLIENT_CERT_FILE}"
		echo "grpc.client.private.key.filepath=${CLIENT_KEY_FILE}"
		if [ -n "${CLIENT_PRIVATE_KEY_PASSWORD}" ]
		then
			echo "grpc.client.private.key.password=${CLIENT_PRIVATE_KEY_PASSWORD}"
		fi
		echo "grpc.override.authority=${OVERRIDE_AUTHORITY}"
	) >>"${GRPC_CLIENT_CONFIG_FILE}.upd"

	mv "${GRPC_CLIENT_CONFIG_FILE}.upd" "${GRPC_CLIENT_CONFIG_FILE}"
fi



###
### EXECUTE THE MINION
###

cd bin

export MINION_ID
export MINION_LOCATION
export USE_KUBERNETES="false"
export IGNITE_SERVER_ADDRESSES
export MINION_GATEWAY_HOST
export MINION_GATEWAY_PORT
export MINION_GATEWAY_TLS

exec ./karaf $DEBUG
