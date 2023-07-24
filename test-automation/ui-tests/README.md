# UI Tests

## Prerequisites

Need to have a Lokahi system running locally, with the following:
- User: `admin`
- Password: `admin`
- URL: `https://onmshs.local:1443`

## Running

The following environment variables can be used to define the system the tests will run against:
- KEYCLOAK_USERNAME - User to log in as
- KEYCLOAK_PASSWORD - Clear text password for the user
- MINION_INGRESS - The host or IP address to connect to the system from the minion. This needs to be the
publicly accessible address of the system, routable from any generated docker containers
in the tests.
- MINION_INGRESS_PORT - The port exposed by the minion ingress
- MINION_INGRESS_OVERRIDE_AUTHORITY - The name to use for auth of the ingress connection,
usually minion.onmshs.local when running locally using tilt.
- MINION_DOCKER_TAG - The docker tag for the minion. Defaults to "latest".
- INGRESS_BASE_URL - The URL for connecting to the UI of the system, often https://onmshs.local:1443 when
running locally with tilt.
- MINION_INGRESS_CA - The file containing the public cert of the minion ingress CA, often in lokahi/target/tmp/server-ca.crt
of the main repo directory when running locally with tilt.
- KEYCLOAK_LOGIN - Set to true when using the default keycloak login, false otherwise


For now, this test suite can be run locally via `mvn test`.

## TODO

- Integration with CI: this suite should be setup to run on events of interest (e.g. PRs, environment update).
- Add more tests! :)
