#!/usr/bin/env bash
set -e 

# defaults
MINION_ID='minion-standalone'
MINION_LOCATION='minion-standalone-loc'
IGNITE_SERVER_ADDRESSES=localhost
MINION_GATEWAY_HOST=minion.onmshs.local
# TODO - port 9443 for TLS?
MINION_GATEWAY_PORT=1443
MINION_GATEWAY_TLS="true"

CERT_ROOTDIR="$(pwd)/target"
CA_CERT_FILE="${CERT_ROOTDIR}/CA.cert"
CLIENT_KEY_FILE="${CERT_ROOTDIR}/client.key"
CLIENT_CERT_FILE="${CERT_ROOTDIR}/client.signed.cert"
CLIENT_KEY_IS_PKCS12="false"



###
### WARNING: certificate passwords currently do not work with PEM files; it only works with PKCS12 files.
###
### The following error is logged when this fails:
###
###	Caused by: java.security.NoSuchAlgorithmException: PBES2 SecretKeyFactory not available
###
CLIENT_PRIVATE_KEY_PASSWORD=""

# Prevent hostname verification failures by setting the expected cert "hostname"
OVERRIDE_AUTHORITY="minion.onmshs.local"

USAGE()
{
	cat <<-!
		Usage: bash $0 [-f <FLA>] [-h <HOST>] [-i <ID>] [-k <PATH>] [-l <LOC>] [-P <PASS>] [-p <PORT>] [-a <ADDRESS>] [-d] [-t]

		    -a[ADDRESS]	use ADDRESS (IGNITE_SERVER_ADDRESSES) configure the Ignite cluster addresses? (warning - this currently may not have any effect)
		    -f[FLAG]	use client private key PKCS12 FLAG (true => PKCS12; false => other)
		    -g[AUTHORITY]	use this to override HOST entry HTTP/2 server authority
		    -h[HOST]	use HOST (MINION_GATEWAY_HOST) as the hostname when connecting to the cloud
		    -i[ID]	use ID (MINION_ID) as the system identifier for this minion instance
		    -k[PATH]	use PATH to the client private key file
		    -l[LOC]	use LOC (MINION_LOCATION) as the name of the location for this minion instance
		    -P[PASS]	use PASS (CLIENT_PRIVATE_KEY_PASSWORD) as the password for the client private key
		    -p[PORT]	use GATEWAY (MINION_GATEWAY_HOST) as the hostname when connecting to the cloud
		    -d		enable jvm debug
		    -t		enable TLS
		    -T		generate minion mTLS from secrets available in local tilt setup (kubectl + openssl)
!
}

while getopts a:f:g:h:i:k:l:P:p:DdtxT FLAG
do
    case "${FLAG}" in
        a) IGNITE_SERVER_ADDRESSES="${OPTARG}" ;;
        f) CLIENT_KEY_IS_PKCS12="${OPTARG}" ;;
        g) OVERRIDE_AUTHORITY="${OPTARG}";;
        h) MINION_GATEWAY_HOST="${OPTARG}" ;;
        i) MINION_ID="${OPTARG}" ;;
        k) CLIENT_KEY_FILE="${OPTARG}" ;;
        l) MINION_LOCATION="${OPTARG}" ;;
        P) CLIENT_PRIVATE_KEY_PASSWORD="${OPTARG}" ;;
        p) MINION_GATEWAY_PORT="${OPTARG}" ;;

        D) DEBUG=debugs ;;
        d) DEBUG=debug ;;
        t) MINION_GATEWAY_TLS="true" ;;
        x) MINION_GATEWAY_TLS="false" ;;
        T) EXTRACT_TILT_CERTS="true" ;;
        ?) USAGE >&2 ; exit 1 ;;
    esac
done

if [ "${EXTRACT_TILT_CERTS}" == "true" ]; then
  mkdir -p $CERT_ROOTDIR || (echo "Could not create $CERT_ROOTDIR" && exit 1)

  # extract client mtls ca certificate
  kubectl get secret client-root-ca-certificate -ogo-template='{{index .data "tls.crt" }}' | base64 --decode > "$CERT_ROOTDIR/client-ca.crt"
  kubectl get secret client-root-ca-certificate -ogo-template='{{index .data "tls.key" }}' | base64 --decode > "$CERT_ROOTDIR/client-ca.key"

  openssl genrsa -out "$CERT_ROOTDIR/client.key.pkcs1" 2048
  openssl pkcs8 -topk8 -in "$CERT_ROOTDIR/client.key.pkcs1" -out "$CLIENT_KEY_FILE" -nocrypt
  openssl req -new -key "$CLIENT_KEY_FILE" -out "$CERT_ROOTDIR/client.unsigned.cert" -subj "/C=CA/ST=TBD/L=TBD/O=OpenNMS/CN=local-minion/OU=L:${MINION_LOCATION}/OU=T:opennms-prime"
  openssl x509 -req -in "$CERT_ROOTDIR/client.unsigned.cert" -days 14 -CA "$CERT_ROOTDIR/client-ca.crt" -CAkey "$CERT_ROOTDIR/client-ca.key" -out "$CLIENT_CERT_FILE" -CAcreateserial

  kubectl get secret root-ca-certificate -ogo-template='{{index .data "ca.crt" }}' | base64 --decode > $CA_CERT_FILE
fi

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
			grep -v '^grpc.client.private.key.is-pkcs12=' |
			grep -v '^grpc.override\.authority='

		echo "grpc.trust.cert.filepath=${CA_CERT_FILE}"
		echo "grpc.client.cert.filepath=${CLIENT_CERT_FILE}"
		echo "grpc.client.private.key.filepath=${CLIENT_KEY_FILE}"
		echo "grpc.client.private.key.is-pkcs12=${CLIENT_KEY_IS_PKCS12}"
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
