#!/bin/sh

set -e

# defaults
MINION_ID='MINION-STANDALONE-DOCKER'
MINION_LOCATION='DOCKER-STANDALONE-LOC'
IGNITE_SERVER_ADDRESSES=localhost
# TODO - port 9443 for TLS?
MINION_GATEWAY_PORT=1443
MINION_GATEWAY_TLS="true"

CERT_ROOTDIR="$(pwd)/target/certs"
CLIENT_KEYSTORE="${CERT_ROOTDIR}/minion.p12"
CLIENT_KEYSTORE_TYPE="pkcs12"
CLIENT_KEYSTORE_PASSWORD="changeme"
CLIENT_TRUSTSTORE="${CERT_ROOTDIR}/CA.cert"
CLIENT_TRUSTSTORE_TYPE="file"
CLIENT_TRUSTSTORE_PASSWORD=""

# Prevent hostname verification failures by setting the expected cert "hostname"
OVERRIDE_AUTHORITY="minion.onmshs.local"

# ARM based MACs use VM which cause no-visibility between actual host and containers.
# The host.docker.internal below have special meaning and can be resolved by containers,
# more over it will be transcribed into actual host address
MINION_GATEWAY_HOST="host.docker.internal"
platform=$(uname)
if [[ $platform == 'Linux' ]]; then
   MINION_GATEWAY_HOST=$(hostname)
fi

if [[ ! -d "${CERT_ROOTDIR}" ]]; then
  mkdir -p "$CERT_ROOTDIR"
fi

USAGE()
{
	cat <<-!
		Usage: bash $0 [-f <FLA>] [-h <HOST>] [-i <ID>] [-k <PATH>] [-l <LOC>] [-P <PASS>] [-p <PORT>] [-a <ADDRESS>] [-d] [-t]

		    -a[ADDRESS]	use ADDRESS (IGNITE_SERVER_ADDRESSES) configure the Ignite cluster addresses? (warning - this currently may not have any effect)
		    -g[AUTHORITY]	use this to override HOST entry HTTP/2 server authority
		    -h[HOST]	use HOST (MINION_GATEWAY_HOST) as the hostname when connecting to the cloud
		    -i[ID]	use ID (MINION_ID) as the system identifier for this minion instance
		    -k[PATH]	use PATH to the client private key file
		    -l[LOC]	use LOC (MINION_LOCATION) as the name of the location for this minion instance
		    -p[PORT]	use GATEWAY (MINION_GATEWAY_HOST) as the hostname when connecting to the cloud
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

        t) MINION_GATEWAY_TLS="true" ;;
        x) MINION_GATEWAY_TLS="false" ;;
        T) EXTRACT_TILT_CERTS="true" ;;
        ?) USAGE >&2 ; exit 1 ;;
    esac
done

docker volume rm minion-certs 2>1 > /dev/null && docker volume create minion-certs

if [ "${EXTRACT_TILT_CERTS}" == "true" ]; then
  mkdir -p $CERT_ROOTDIR || (echo "Could not create $CERT_ROOTDIR" && exit 1)

  CA_CERT_FILE="${CERT_ROOTDIR}/CA.cert"
  CLIENT_KEY_FILE="${CERT_ROOTDIR}/client.key"
  CLIENT_CERT_FILE="${CERT_ROOTDIR}/client.signed.cert"

  # extract client mtls ca certificate
  kubectl get secret client-root-ca-certificate -ogo-template='{{index .data "tls.crt" }}' | base64 --decode > "$CERT_ROOTDIR/client-ca.crt"
  kubectl get secret client-root-ca-certificate -ogo-template='{{index .data "tls.key" }}' | base64 --decode > "$CERT_ROOTDIR/client-ca.key"

  openssl genrsa -out "$CERT_ROOTDIR/client.key.pkcs1" 2048
  openssl pkcs8 -topk8 -in "$CERT_ROOTDIR/client.key.pkcs1" -out "$CLIENT_KEY_FILE" -nocrypt
  openssl req -new -key "$CLIENT_KEY_FILE" -out "$CERT_ROOTDIR/client.unsigned.cert" -subj "/C=CA/ST=TBD/L=TBD/O=OpenNMS/CN=local-minion/OU=L:${MINION_LOCATION}/OU=T:opennms-prime"
  openssl x509 -req -in "$CERT_ROOTDIR/client.unsigned.cert" -days 14 -CA "$CERT_ROOTDIR/client-ca.crt" -CAkey "$CERT_ROOTDIR/client-ca.key" -out "$CLIENT_CERT_FILE" -CAcreateserial
  openssl pkcs12 -export -out "${CLIENT_KEYSTORE}" -inkey "$CLIENT_KEY_FILE" -in "$CLIENT_CERT_FILE" -passout "pass:${CLIENT_KEYSTORE_PASSWORD}"

  kubectl get secret root-ca-certificate -ogo-template='{{index .data "ca.crt" }}' | base64 --decode > $CA_CERT_FILE

  DOCKER_KEYSTORE="/opt/karaf/certs/minion.p12"
  DOCKER_TRUSTSTORE="/opt/karaf/certs/CA.cert"

  # create fresh volume to store certs with owner set to user running minion
  docker volume rm minion-certs 2>1 > /dev/null && docker volume create minion-certs
  docker rm -f temp 2>1 > /dev/null #just in case if earlier invocation failed for any reason
  # copy certificates into volume
  docker run -d --rm --name temp -v minion-certs:/opt/karaf/certs alpine tail -f /dev/null
  docker cp "${CLIENT_KEYSTORE}" "temp:${DOCKER_KEYSTORE}"
  docker cp "${CLIENT_TRUSTSTORE}" "temp:${DOCKER_TRUSTSTORE}"
  docker exec -it temp sh -c "chown -R 10001:10001 /opt/karaf/certs"
  docker rm -f temp

  CLIENT_KEYSTORE="${DOCKER_KEYSTORE}"
  CLIENT_TRUSTSTORE="${DOCKER_TRUSTSTORE}"
fi

#
#
#
#--mount "type=bind,source=${CERT_ROOTDIR},target=/opt/karaf/certs/" \

exec docker run \
                --rm \
                -it \
                -e GRPC_CERT_OVERRIDE_AUTHORITY=${OVERRIDE_AUTHORITY} \
                -e MINION_GATEWAY_HOST="${MINION_GATEWAY_HOST}" \
                -e MINION_GATEWAY_PORT="${MINION_GATEWAY_PORT}" \
                -e MINION_ID="${MINION_ID}" \
                -e MINION_LOCATION="${MINION_LOCATION}" \
                -e MINION_GATEWAY_TLS="${MINION_GATEWAY_TLS}" \
                -e USE_KUBERNETES="false" \
                -e GRPC_CLIENT_KEYSTORE="${CLIENT_KEYSTORE}" \
                -e GRPC_CLIENT_KEYSTORE_TYPE="${CLIENT_KEYSTORE_TYPE}" \
                -e GRPC_CLIENT_KEYSTORE_PASSWORD="${CLIENT_KEYSTORE_PASSWORD}" \
                -e GRPC_CLIENT_TRUSTSTORE="${CLIENT_TRUSTSTORE}" \
                -e GRPC_CLIENT_TRUSTSTORE_TYPE="${CLIENT_TRUSTSTORE_TYPE}" \
                -e GRPC_CLIENT_TRUSTSTORE_PASSWORD="${CLIENT_TRUSTSTORE_PASSWORD}" \
                -e GRPC_CLIENT_OVERRIDE_AUTHORITY="${OVERRIDE_AUTHORITY}" \
                -v "minion-certs:/opt/karaf/certs" \
                opennms/horizon-stream-minion:tilt-be02473c2f7da49a \
		console
