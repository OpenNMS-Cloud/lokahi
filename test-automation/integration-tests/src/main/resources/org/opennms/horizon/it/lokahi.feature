Feature: Minion Monitoring via Echo Messages Logged in Prometheus

  Background: Login to Keycloak
    Given Ingress base url in environment variable "INGRESS_BASE_URL"
    Given Keycloak server base url in environment variable "KEYCLOAK_BASE_URL"
    Given Keycloak realm in environment variable "KEYCLOAK_REALM"
    Given Keycloak username in environment variable "KEYCLOAK_USERNAME"
    Given Keycloak password in environment variable "KEYCLOAK_PASSWORD"
    Given Keycloak client-id in environment variable "KEYCLOAK_CLIENT_ID"
    Given Minion image name in environment variable "MINION_IMAGE_NAME"
    Given Minion ingress base url in environment variable "MINION_INGRESS_URL"
    Given Minion ingress port is in variable "MINION_INGRESS_PORT"
    Given Minion ingress TLS enabled flag is in variable "MINION_INGRESS_TLS"
    Given Minion ingress CA certificate file is in environment variable "MINION_INGRESS_CA"
    Given Minion ingress overridden authority is in variable "MINION_INGRESS_OVERRIDE_AUTHORITY"
    Then login to Keycloak with timeout 120000ms
    Given Location "TestLocation" is created

  Scenario: Create "External" location and request Minion certificate
    Given No Minion running with location "External"
    #Given Location "External" does not exist
    When Location "External" is created
    Then Location "External" do exist
    Then Request certificate for location "External"
    When Minion "test-minion" is started in location "External"
    Given At least one Minion is running with location "External"
    Then Wait for at least one minion for the given location reported by inventory with timeout 180000ms
    When Location "External" is removed
    Then Location "External" does not exist
    Then Minion "test-minion" is stopped

  Scenario: Wait for at least one minion to connect from location TestLocation
    Given At least one Minion is running with location "TestLocation"
    # NOTE: there is redundant processing between this step and the ones that follow it
    Then Wait for at least one minion for the given location reported by inventory with timeout 600000ms

  Scenario: Verify Minion echo measurements are recorded into prometheus for a running Minion
    Given At least one Minion is running with location "TestLocation"
    Then Wait for at least one minion for the given location reported by inventory with timeout 600000ms
    Then Read the list of connected Minions from the BFF
    Then Find the minions running in the given location
    Then Verify at least one minion was found for the location
    Then Read the "response_time_msec" metrics with label "instance" set to the Minion System ID for each Minion found with timeout 120000ms

  Scenario: Add devices and verify monitoring metrics are recorded into prometheus
    Given At least one Minion is running with location "TestLocation"
    Then Wait for at least one minion for the given location reported by inventory with timeout 600000ms
    Then Add a device with label "local1" IP address "127.1.0.1" and location "TestLocation"
    Then Add a device with label "local2" IP address "127.1.0.2" and location "TestLocation"
    Then Add a device with label "local3" IP address "127.1.0.3" and location "TestLocation"
    Then Read the "response_time_msec" metrics with label "instance" set to "127.1.0.1" with timeout 120000ms
    Then Read the "response_time_msec" metrics with label "instance" set to "127.1.0.2" with timeout 120000ms
    Then Read the "response_time_msec" metrics with label "instance" set to "127.1.0.3" with timeout 120000ms
    Then Delete the first node from inventory
    Then Delete the first node from inventory
    Then Delete the first node from inventory

  Scenario: Create a Node and check it status
    Given At least one Minion is running with location "TestLocation"
    Then Add a device with label "NodeUp" IP address "127.1.0.4" and location "TestLocation"
    Then Check the status of the Node with expected status "UP"
    Then Delete the first node from inventory
    Then Add a device with label "NodeDown" IP address "192.168.0.4" and location "TestLocation"
    Then Check the status of the Node with expected status "DOWN"
    Then Delete the first node from inventory

  Scenario: Create discovery and check the status of the discovered node
    Given At least one Minion is running with location "TestLocation"
    Then Wait for at least one minion for the given location reported by inventory with timeout 600000ms
    # Currently this test is using Minion open port 161 to make a discovery. In future would be preferred to use container with open ports
    Then Add a new active discovery for the name "Automation Discovery Tests" at location "TestLocation" with ip address "127.1.0.5" and port 161, readCommunities "public"
    Then Check the status of the Node with expected status "UP"
    Then Delete the first node from inventory
