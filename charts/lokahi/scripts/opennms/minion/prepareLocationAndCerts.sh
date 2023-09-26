#!/usr/bin/env bash
######################################################################################################################
##
## DESCRIPTION:
##	Prepare for running a minion by logging into the cluster, looking up the ID of the location name requested,
##	and creating the location if is does not yet exist, then downloading the PKCS12 Minion certficiate.
##
## PROCESS:
##	1. Extract the CA certificate from the cluster and save as the trust-store for the Minion (and curl commands)
##	2. Login to the cluster
##	3. Lookup the location
##	4. If the location does not yet exist, create the location
##	5. Download the location certificate and password
##
## EXAMPLE USAGE:
##	./prepareLocationAndCerts.sh
##	./prepareLocationAndCerts.sh -l my-minion-location
##
######################################################################################################################


TEMPLATE_LOOKUP_LOCATION_GQL='
	query { locationByName(locationName: "%s") { id } }
	'

TEMPLATE_CREATE_LOCATION_GQL='
	mutation createLocation { createLocation(location: { location: "%s" }) { id } }
	'

TEMPLATE_GET_CERTIFICATE_GQL='
	query { getMinionCertificate(locationId: %d) { certificate, password } }
'


###
### SCRIPT INPUTS
###

CERT_ROOTDIR="$(pwd)/target"
CLIENT_KEYSTORE="${CERT_ROOTDIR}/minion.p12"
CLIENT_KEYSTORE_PASSWORD="" # default: keep the original generated password
CLIENT_TRUSTSTORE="${CERT_ROOTDIR}/CA.cert"
INSECURE="false"
API_BASE_URL=https://onmshs.local
LOCATION_NAME="default"
USERNAME="admin"
PASSWORD="admin"
VERBOSE="false"
AUTH_REALM="opennms"
CLIENT_ID="lokahi"
CURL_ARGS=()



###
### RUNTIME DATA
###

ACCESS_TOKEN=""
LOCATION_ID=-1



###
###
###

is_verbose ()
{
	if [ "$VERBOSE" = "true" ]
	then
		return 0
	else
		return 1
	fi
}

format_lookup_location_gql_query ()
{
	typeset location_name

	location_name="$1"

	printf "${TEMPLATE_LOOKUP_LOCATION_GQL}" "${location_name}"
}

format_create_location_gql_query ()
{
	typeset location_name

	location_name="$1"

	printf "${TEMPLATE_CREATE_LOCATION_GQL}" "${location_name}"
}

format_get_ceritificate_gql_query ()
{
	typeset location_id

	location_id="$1"

	printf "${TEMPLATE_GET_CERTIFICATE_GQL}" "${location_id}"
}

format_graphql_url ()
{
	echo "${API_BASE_URL}/api/graphql"
}

format_auth_url ()
{
	typeset realm

	realm="$1"

	echo "${API_BASE_URL}/auth/realms/${realm}/protocol/openid-connect/token"
}

gql_result_check_no_errors ()
{
	typeset gql_response
	typeset report_errors_flag

	typeset err_msg

	gql_response="$1"
	report_errors_flag="${2:-false}"

	# Check for an error message
	err_msg="$(echo "${gql_response}" | jq 'select(.errors) | .errors')"

	if [ -z "$err_msg" ]
	then
		# Also check for .error
		err_msg="$(echo "${gql_response}" | jq -r 'select(.error) | .error')"
	fi

	if [ -n "$err_msg" ]
	then
		# Have errors

		if [ "${report_errors_flag}" != "false" ] || is_verbose
		then
			echo "ERROR: GRAPHQL QUERY FAILED" >&2
			echo "${gql_response}" | jq . >&2
		fi

		return 1
	fi

	return 0
}

execute_gql_query ()
{
	typeset gql_query
	typeset gql_response
	typeset gql_url
	typeset gql_formatted_query_envelope
	typeset report_errors_flag

	gql_query="$1"
	report_errors_flag="${2:-}"

	gql_url="$(format_graphql_url)"

	if is_verbose
	then
		echo "=== DEBUG: Sending GQL Query: url=${gql_url}; query=${gql_query}" >&2
	fi

	gql_formatted_query_envelope="$(echo "$gql_query" | jq -R 'select(length > 0) | { query: . }')"

	gql_response="$(
		curl \
			-S \
			-s \
			-f \
			-X POST \
			-H 'Content-Type: application/json' \
			-H "Authorization: Bearer ${ACCESS_TOKEN}" \
			--data-ascii "${gql_formatted_query_envelope}" \
			"${CURL_ARGS[@]}" \
			"${gql_url}"
		)"

	if is_verbose
	then
		echo "=== DEBUG: GQL Response: response=${gql_response}" >&2
	fi

	if gql_result_check_no_errors "${gql_response}" "${report_errors_flag}"
	then
		echo "$gql_response"
	else
		return 1
	fi
}

login ()
{
	typeset auth_url
	typeset response
	typeset error

	auth_url="$(format_auth_url "${AUTH_REALM}")"

	echo ">>> LOGIN USER ${USERNAME}"

	response="$(
		curl \
			-S \
			-s \
			-f \
			-X POST \
			-H 'Content-Type: application/x-www-form-urlencoded' \
			-d "username=${USERNAME}" \
			-d "password=${PASSWORD}" \
			-d 'grant_type=password' \
			-d "client_id=${CLIENT_ID}" \
			-d 'scope=openid' \
			"${CURL_ARGS[@]}" \
			"${auth_url}"
	)"

	error="$(echo "$response" | jq -r 'select(.error) | .error')"
	if [ -n "$error" ]
	then
		echo "!!! Login failed"
		echo "$response"
		exit 1
	fi

	ACCESS_TOKEN="$(echo "$response" | jq -r '.access_token')"

	if [ -z "${ACCESS_TOKEN}" ]
	then
		echo "!!! Login error - failed to extract access token from the response"
		echo "$response"
		exit 1
	fi

	if is_verbose
	then
		echo "ACCESS TOKEN = ${ACCESS_TOKEN}" >&2
	fi
}

lookup_location ()
{
	typeset location_name
	typeset gql_query
	typeset gql_response
	typeset gql_url

	location_name="$1"

	echo ">>> LOOKUP LOCATION ${location_name}"

	gql_query="$(format_lookup_location_gql_query "${location_name}")"

	if gql_response="$(execute_gql_query "${gql_query}")"
	then
		LOCATION_ID="$(echo "${gql_response}" | jq -r '.data.locationByName.id')"
		echo "Have location ${location_name}, ID=${LOCATION_ID}"

		return 0
	else
		return 1
	fi
}

create_location ()
{
	typeset location_name
	typeset gql_query
	typeset gql_response

	location_name="$1"

	echo ">>> CREATING LOCATION ${location_name}"

	gql_query="$(format_create_location_gql_query "${location_name}")"

	if gql_response="$(execute_gql_query "${gql_query}")"
	then
		LOCATION_ID="$(echo "${gql_response}" | jq -r '.data.createLocation.id')"
		echo "Have location ${location_name}, ID=${LOCATION_ID}"

		return 0
	else
		echo "Failed to create location ${location_name}; aborting" >&2
		exit 1
	fi
}

retrieve_certificate ()
{
	typeset location_id

	typeset gql_query
	typeset gql_response

	location_id="$1"

	echo ">>> RETRIEVING CERTIFICATE FOR LOCATION ID ${location_id}"

	gql_query="$(format_get_ceritificate_gql_query "${location_id}")"

	if gql_response="$(execute_gql_query "${gql_query}")"
	then
		CERTIFICATE_DATA="$(echo "${gql_response}" | jq -r '.data.getMinionCertificate.certificate')"
		CERTIFICATE_PASSWORD="$(echo "${gql_response}" | jq -r '.data.getMinionCertificate.password')"

		return 0
	else
		echo "Failed to retrieve certificate for location id ${location_id}; aborting" >&2
		exit 1
	fi
}

print_p12_subject ()
{
	typeset p12_path
	typeset p12_password

	p12_path="$1"
	p12_password="$2"

	openssl pkcs12 -in "${p12_path}" -nodes -passin pass:"${p12_password}"  | openssl x509 -noout -subject
}

store_certificate ()
{
	if [ -f "${CLIENT_KEYSTORE}" ]
	then
		mv -f "${CLIENT_KEYSTORE}" "${CLIENT_KEYSTORE}.bak"
	fi

	echo "${CERTIFICATE_DATA}" | base64 --decode >"/tmp/cert.zip"
        unzip -o -p /tmp/cert.zip storage/minion1-default.p12 >"${CLIENT_KEYSTORE}"
}

get_ca_cert_from_k8s ()
{
	if [ "$INSECURE" = "true" ]; then
		return # we don't need to bother getting the CLIENT_TRUSTSTORE from kubectl
	fi

	echo ">>> EXTRACTING client truststore contents from K8S"

	if [ -f "${CLIENT_TRUSTSTORE}" ]
	then
		mv -f "${CLIENT_TRUSTSTORE}" "${CLIENT_TRUSTSTORE}.bak"
	fi

	kubectl get secret root-ca-certificate -o go-template='{{index .data "ca.crt" }}' | base64 --decode > "${CLIENT_TRUSTSTORE}"

	openssl x509 -in target/CA.cert -subject -noout
}

show_command_line_help ()
{
	echo "Usage: $0 [-h] [-v] [-k] [-l location] [-u username] [-p password] [-U URL] [-c arg] [-f file] [-P pass]"
	echo
	echo "	-h	Display this help"
	echo "	-v 	Enable verbose mode"
	echo "	-k 	Don't get client truststore, pass insecure (-k) flag to curl"
	echo "	-l loc	Name of the location to use/create (default: ${LOCATION_NAME})"
	echo "	-u user	Username for logging into the cluster"
	echo "	-p pass	Password for logging into the cluster"
	echo "	-U URL	API base URL (default: ${API_BASE_URL})"
	echo "	-c arg	curl arguments (you can use multiple times)"
	echo "	-f file	Output .p12 file (default: ${CLIENT_KEYSTORE})"
	echo "	-P pass	Change p12 password to this password (default: keep original password)"
}

parse_command_line ()
{
	while getopts f:hvkl:p:P:u:U:c: FLAG
	do
		case "$FLAG" in
			h)	show_command_line_help; exit 0 ;;
			v)	VERBOSE="true" ;;
			k)	INSECURE="true" ;;
			l)	LOCATION_NAME="${OPTARG}" ;;
			u)	USERNAME="${OPTARG}" ;;
			p)	PASSWORD="${OPTARG}" ;;
			U)	API_BASE_URL="${OPTARG}" ;;
			c)	CURL_ARGS+=("${OPTARG}") ;;
			f)	CLIENT_KEYSTORE="${OPTARG}" ;;
			P)	CLIENT_KEYSTORE_PASSWORD="${OPTARG}" ;;
			?)	show_command_line_help >&2; exit 1 ;;
		esac
	done
}

setup_curl_args ()
{
	# Prepend our args here so the user can override anything if needed
	if [ "$INSECURE" = "true" ];
	then
		CURL_ARGS=("-k" "${CURL_ARGS[@]}")
	else
		CURL_ARGS=("--cacert=${CLIENT_TRUSTSTORE}" "${CURL_ARGS[@]}")
	fi
}



######################################################################################################################
##
## MAIN BODY STARTS HERE
##
######################################################################################################################

# https://github.com/olivergondza/bash-strict-mode
set -eEuo pipefail
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; exit $s' ERR

parse_command_line "$@"

setup_curl_args

get_ca_cert_from_k8s

login

if lookup_location "${LOCATION_NAME}"
then
	:
else
	create_location "${LOCATION_NAME}"
fi

retrieve_certificate "${LOCATION_ID}"

store_certificate "${CLIENT_KEYSTORE}"

print_p12_subject "${CLIENT_KEYSTORE}" "${CERTIFICATE_PASSWORD}"

if [ -n "${CLIENT_KEYSTORE_PASSWORD}" ]; then
	keytool -importkeystore -srckeystore "${CLIENT_KEYSTORE}" -srcstoretype PKCS12 -srcstorepass "${CERTIFICATE_PASSWORD}" \
		-destkeystore "${CLIENT_KEYSTORE}.new" -deststoretype PKCS12 -storepass "${CLIENT_KEYSTORE_PASSWORD}"
	mv "${CLIENT_KEYSTORE}.new" "${CLIENT_KEYSTORE}"
	CERTIFICATE_PASSWORD="${CLIENT_KEYSTORE_PASSWORD}"
fi

echo "Certificate File = ${CLIENT_KEYSTORE}"
echo "Certificate Password = ${CERTIFICATE_PASSWORD}"
