# Tools

## Create the CA certificate and key:

./mk-self-signed-CA

## Create a server certificate and key, and sign the certificate with the CA:

./mk-signed-server-cert

## Create a client certificate and key, and sign the certificate with the CA:

./mk-signed-client-cert

## Certificate Files

| File                 | Description                         |
|----------------------|-------------------------------------|
| CA.cert              | CA certificate                      |
| CA.key               | CA private key                      |
| client.key           | Client private key (PKCS8 format)   |
| client.key.pkcs1     | Client private key (PKCS1 format)   | 
| client.signed.cert   | Client certificate signed by the CA |
| client.unsigned.cert | Client unsigned certificate         |
| server.key           | Server private key                  |
| server.signed.cert   | Server certificate signed by the CA |
| server.unsigned.cert | Server unsigned certificate         |

