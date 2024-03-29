#!/bin/bash

HOST_PORT=localhost:8181
ROOT_PATH=""
JSON="application/json"
XML="application/xml"

BODY_TEMPLATE='{
  "user": "%s",
  "ticketId": "%s",
  "ticketState": "%s"
}'

ACK_USER="user001"
ACK_TICKET_ID="ticket001"
ACK_TICKET_STATE="RESOLVED"
ALARM_ID=1
CURL_OPTS=()
TOKEN=""

while getopts "cH:i:t:u:v" arg
do
	case "$arg" in
		c)
			ROOT_PATH="cxf/"
			;;

		H)
			HOST_PORT="${OPTARG}"
			;;

		i)
			ALARM_ID="${OPTARG}"
			;;

		t)
			TOKEN="${OPTARG}"
			CURL_OPTS=("${CURL_OPTS[@]}" "-H" "Authorization: Bearer ${OPTARG}")
			;;

		u)
			CURL_OPTS=("${CURL_OPTS[@]}" "-u${OPTARG}")
			;;

		v)
			CURL_OPTS=("${CURL_OPTS[@]}" -v)
			;;
	esac
done

BODY="$(printf "${BODY_TEMPLATE}" "${ACK_USER}" "${ACK_TICKET_ID}" "${ACK_TICKET_STATE}")"

curl "${CURL_OPTS[@]}" -X POST --data-ascii "${BODY}" -H "Content-Type: ${JSON}" -s "http://${HOST_PORT}/${ROOT_PATH}alarms/${ALARM_ID}/ack"
