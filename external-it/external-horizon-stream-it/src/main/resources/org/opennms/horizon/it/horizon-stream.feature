Feature: Minion Monitoring via Echo Messages Logged in Prometheus

  Background: Login to Keycloak
    Given Ingress base url in environment variable "INGRESS_BASE_URL"
    Given Keycloak server base url in environment variable "KEYCLOAK_BASE_URL"
    Given Keycloak realm in environment variable "KEYCLOAK_REALM"
    Given Keycloak username in environment variable "KEYCLOAK_USERNAME"
    Given Keycloak password in environment variable "KEYCLOAK_PASSWORD"
    Given Keycloak client-id in environment variable "KEYCLOAK_CLIENT_ID"
    Then login to Keycloak with timeout 120000ms

  Scenario: Wait for at least one minion to connect from location Default
    Given At least one Minion is running with location "Default"
    # NOTE: there is redundant processing between this step and the ones that follow it
    Then Wait for at least one minion for the given location reported by inventory with timeout 600000ms

  Scenario: Verify Minion echo measurements are recorded into prometheus for a running Minion
    Given At least one Minion is running with location "Default"
    Then Read the list of connected Minions from the BFF
    Then Find the minions running in the given location
    Then Verify at least one minion was found for the location
    Then Read the "response_time_msec" from Prometheus with label "system_id" set to the Minion System ID for each Minion found with timeout 120000ms
