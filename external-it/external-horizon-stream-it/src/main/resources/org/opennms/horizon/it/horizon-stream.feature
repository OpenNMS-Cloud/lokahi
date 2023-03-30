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
    Then Read the "response_time_msec" metrics with label "instance" set to the Minion System ID for each Minion found with timeout 120000ms

  Scenario: Add devices and verify monitoring metrics are recorded into prometheus
    Given At least one Minion is running with location "Default"
    Then Add a device with label "local1" IP address "127.1.0.1" and location "Default"
    Then Add a device with label "local2" IP address "127.1.0.2" and location "Default"
    Then Add a device with label "local3" IP address "127.1.0.3" and location "Default"
    Then Read the "response_time_msec" metrics with label "instance" set to "127.1.0.1" with timeout 120000ms
    Then Read the "response_time_msec" metrics with label "instance" set to "127.1.0.2" with timeout 120000ms
    Then Read the "response_time_msec" metrics with label "instance" set to "127.1.0.3" with timeout 120000ms
    Then Delete the first node from inventory
    Then Delete the first node from inventory
    Then Delete the first node from inventory

  Scenario: Create a Node and check it status
    Then Add a device with label "NodeUp" IP address "127.1.0.4" and location "Default"
    Then Check the status of the Node with expected status "UP"
    Then Delete the first node from inventory
    Then Add a device with label "NodeDown" IP address "192.168.0.4" and location "Default"
    Then Check the status of the Node with expected status "DOWN"
    Then Delete the first node from inventory

  Scenario: Create discovery and check the status of the discovered node
    # Currently this test is using Minion open port 161 to make a discovery. In future would be preferred to use container with open ports
    Then Add a new active discovery for the name "Automation Discovery Tests" at location "Default" with ip address "127.1.0.5" and port 161, readCommunities "public"
    Then Check the status of the Node with expected status "UP"
    Then Delete the first node from inventory
