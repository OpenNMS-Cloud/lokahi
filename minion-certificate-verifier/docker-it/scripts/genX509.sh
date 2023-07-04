#!/bin/bash

CA_KEY=../../../target/tmp/client-ca.key
CA_CRT=../../../target/tmp/client-ca.crt
TMP_KEY=/tmp/client.key
UNSIGNED_CRT=/tmp/client.unsigned.cert
SUBJECT=$(echo $1 | sed -e 's/,/\//g')


if [ ! -f $TMP_KEY ]; then
  openssl genrsa -out $TMP_KEY 2048
fi

echo $SUBJECT
openssl req -new -key $TMP_KEY -out $UNSIGNED_CRT -subj "/C=CA/ST=ON/L=Ottawa/O=opennms/${SUBJECT}"
X509=$(openssl x509 -req -in $UNSIGNED_CRT -days 3650 -CA $CA_CRT -CAkey $CA_KEY)
printf %s "$X509"|jq -sRr @uri
