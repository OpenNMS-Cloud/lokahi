# To Use

    * With Lokahi running on localhost at port 8080
    * Keycloak on port 9000
    * Keycloak user "user001", password "passw0rd", in realm "opennms"

    $ mvn clean install
    $ INGRESS_BASE_URL=http://localhost:8123
    $ KEYCLOAK_BASE_URL=http://localhost:8123/auth
    $ KEYCLOAK_REALM=opennms
    $ KEYCLOAK_USERNAME=user001
    $ KEYCLOAK_PASSWORD=passw0rd
    $ KEYCLOAK_CLIENT_ID=lokahi
    $ export INGRESS_BASE_URL HORIZON_STREAM_BASE_URL KEYCLOAK_BASE_URL KEYCLOAK_REALM KEYCLOAK_USERNAME KEYCLOAK_PASSWORD KEYCLOAK_CLIENT_ID
    $ PROJECT_VERSION="$(mvn -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive -q org.codehaus.mojo:exec-maven-plugin:1.6.0:exec)"
    $ java -jar "target/integration-tests-${PROJECT_VERSION}.jar"

    * When running with tilt:
    * Get the ingress ca.crt from the secret "opennms-minion-gateway-certificate" (need to base64 decode the k8s secret)
    * Env vars for running with tilt:

    $ export INGRESS_BASE_URL=https://onmshs.local:1443
    $ export KEYCLOAK_BASE_URL=https://onmshs.local:1443/auth
    $ export KEYCLOAK_REALM=opennms
    $ export KEYCLOAK_USERNAME=admin
    $ export KEYCLOAK_PASSWORD=admin
    $ export KEYCLOAK_CLIENT_ID=lokahi
    $ export PROJECT_VERSION="$(mvn -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive -q org.codehaus.mojo:exec-maven-plugin:1.6.0:exec)"
    $ export MINION_INGRESS_PORT=1443
    $ export MINION_INGRESS_URL=<your public IP of the local system>
    $ export MINION_INGRESS_TLS=true
    $ export MINION_INGRESS_CA=<path to downloaded ingress ca.crt>
    $ export MINION_INGRESS_OVERRIDE_AUTHORITY=minion.onmshs.local
    $ export MINION_IMAGE_NAME=opennms/lokahi-minion:latest

