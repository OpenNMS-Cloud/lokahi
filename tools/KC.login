#!/bin/bash
########################################################################################################################
##
## PROGRAM: KC.login
##
## PURPOSE:
##	Login to Keycloak with username + password and remember the Access Token and Refresh Token.
##	
## FILES:
##	TOKEN.txt - the full text of the server response
##	ACCESS_TOKEN.txt - the access_token key returned by the server
##	REFRESH_TOKEN.txt - the refresh_token key returned by the server
##
########################################################################################################################

HOST_PORT=localhost:8123
REALM=opennms
CLIENT_ID=admin-cli
USERNAME=admin
PASSWORD=admin
CONTEXT="auth/"

usage()
{
	echo "Usage: $0 [-hv] [-H <host:port>] [-c <client-id>] [-p <password>] [-R <realm>] [-u <username>]"
	echo
	echo "  -h  Display this help"
	echo "  -v  Verbose curl output"
}

while getopts "C:c:H:hp:R:u:v" arg
do
	case "$arg" in
		C)
			CONTEXT="${OPTARG}"
			;;

		c)
			CLIENT_ID="${OPTARG}"
			;;

		H)
			HOST_PORT="${OPTARG}"
			;;

		h)
			usage
			exit 0
			;;

		p)
			PASSWORD="${OPTARG}"
			;;

		R)
			REALM="${OPTARG}"
			;;

		u)
			USERNAME="${OPTARG}"
			;;

		v)
			CURL_OPTS=("${CURL_OPTS[@]}" -v)
			;;

		*)
			usage >&2
			exit 1
			;;
	esac
done

URL="http://${HOST_PORT}/${CONTEXT}realms/${REALM}/protocol/openid-connect/token"

{
	curl "${CURL_OPTS[@]}" -X POST \
		-H 'Content-Type: application/x-www-form-urlencoded' \
		-d "username=${USERNAME}" \
		-d "password=${PASSWORD}" \
		-d 'grant_type=password' \
		-d "client_id=${CLIENT_ID}" \
		-d 'scope=openid' \
		-s \
		"${URL}"
} | \
	tee data/TOKEN.txt

jq -r .access_token data/TOKEN.txt | tee data/ACCESS_TOKEN.txt
jq -r .refresh_token data/TOKEN.txt | tee data/REFRESH_TOKEN.txt
