# To Use

    * With Horizon Stream running on localhost at port 8080
    * Keycloak on port 9000
    * Keycloak user "user001", password "passw0rd", in realm "opennms"

    $ mvn clean install
    $ INGRESS_BASE_URL=http://localhost:8123
    $ KEYCLOAK_BASE_URL=http://localhost:8123/auth
    $ KEYCLOAK_REALM=opennms
    $ KEYCLOAK_USERNAME=user001
    $ KEYCLOAK_PASSWORD=passw0rd
    $ KEYCLOAK_CLIENT_ID=horizon-stream
    $ export HORIZON_STREAM_BASE_URL KEYCLOAK_BASE_URL KEYCLOAK_REALM KEYCLOAK_USERNAME KEYCLOAK_PASSWORD
    $ PROJECT_VERSION="$(mvn -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive -q org.codehaus.mojo:exec-maven-plugin:1.6.0:exec)"
    $ java -jar "external-horizon-stream-it/target/external-horizon-stream-it-${PROJECT_VERSION}.jar"

