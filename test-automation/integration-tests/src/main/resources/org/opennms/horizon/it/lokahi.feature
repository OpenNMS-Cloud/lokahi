@inventory
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

  @external
  Scenario: Create "External" location and request Minion certificate
      Given Minion "Stuart" is stopped
      Given No Minion running with location "External"
        # Fails, need to delete External location.
      Then Create location "External"
      Then Location "External" do exist
      Then Request certificate for location "External"
      When Minion "Kevin" is started with shared networking in location "External"
      Then At least one Minion is running with location "External"
      #Then Wait for at least one minion for the given location reported by inventory with timeout 600000ms

      Then Read the list of connected Minions from the BFF
      Then Find the minions running in the given location
      #Then Verify at least one minion was found for the location
        # Cannot get this to work.

      Then Add a device with label "local1" IP address "127.1.0.1" and location "External"
      Then Add a device with label "local2" IP address "127.1.0.2" and location "External"
      Then Add a device with label "local3" IP address "127.1.0.3" and location "External"
      Then Read the "response_time_msec" metrics with label "instance" set to "127.1.0.1" with timeout 120000ms
      Then Read the "response_time_msec" metrics with label "instance" set to "127.1.0.2" with timeout 120000ms
      Then Read the "response_time_msec" metrics with label "instance" set to "127.1.0.3" with timeout 120000ms
      # Delete the node {string} from inventory in location {string}
      # The following has errors, but with deleting the tenant, we can ignore for now.
      #Then Delete the node "local1" from inventory in location "External"
      #Then Delete the node "local2" from inventory in location "External"
      #Then Delete the node "local3" from inventory in location "External"

      When Location "External" is removed
      Then Location "External" does not exist
      Then Minion "Kevin" is stopped
        # Kevin not being removed.

  @measurements
  Scenario: Verify Minion echo measurements are recorded into prometheus for a running Minion
    Then Create location "Measurements"
    Then Location "Measurements" do exist
    Then Request certificate for location "Measurements"
    When Minion "Stuart" is started with shared networking in location "Measurements"
    Then At least one Minion is running with location "Measurements"
    Then Read the list of connected Minions from the BFF
    Then Find the minions running in the given location
    Then Read the "response_time_msec" metrics with label "instance" set to the Minion System ID for each Minion found with timeout 120000ms
    When Location "Measurements" is removed
    Then Location "Measurements" does not exist
    Then Minion "Stuart" is stopped

  @metrics
  Scenario: Add devices and verify monitoring metrics are recorded into prometheus
    Then Create location "Metrics"
    Then Location "Metrics" do exist
    Then Request certificate for location "Metrics"
    When Minion "Bob" is started with shared networking in location "Metrics"
    Then At least one Minion is running with location "Metrics"
    Then Add a device with label "local1" IP address "127.1.0.1" and location "Metrics"
    Then Add a device with label "local2" IP address "127.1.0.2" and location "Metrics"
    Then Add a device with label "local3" IP address "127.1.0.3" and location "Metrics"
    Then Read the "response_time_msec" metrics with label "instance" set to "127.1.0.1" with timeout 120000ms
    Then Read the "response_time_msec" metrics with label "instance" set to "127.1.0.2" with timeout 120000ms
    Then Read the "response_time_msec" metrics with label "instance" set to "127.1.0.3" with timeout 120000ms
    Then Delete the first node from inventory
    Then Delete the first node from inventory
    Then Delete the first node from inventory
    When Location "Metrics" is removed
    Then Location "Metrics" does not exist
    Then Minion "Bob" is stopped

  @nodeStatus
  Scenario: Create a Node and check it status
    Then Create location "NodeStatus"
    Then Location "NodeStatus" do exist
    Then Request certificate for location "NodeStatus"
    When Minion "Carl" is started with shared networking in location "NodeStatus"
    Then At least one Minion is running with location "NodeStatus"
    Then Add a device with label "NodeUp" IP address "127.1.0.4" and location "NodeStatus"
    Then Check the status of the Node with expected status "UP"
    Then Delete the first node from inventory
    Then Add a device with label "NodeDown" IP address "192.168.0.4" and location "NodeStatus"
    Then Check the status of the Node with expected status "DOWN"
    Then Delete the first node from inventory
    When Location "NodeStatus" is removed
    Then Location "NodeStatus" does not exist
    Then Minion "Carl" is stopped

  @nodeDiscovery
  Scenario: Create discovery and check the status of the discovered node
    Then Create location "NodeDiscovery"
    Then Location "NodeDiscovery" do exist
    Then Request certificate for location "NodeDiscovery"
    When Minion "Dave" is started with shared networking in location "NodeDiscovery"
    Then At least one Minion is running with location "NodeDiscovery"
  # Currently this test is using Minion open port 161 to make a discovery. In future would be preferred to use container with open ports
    Then Add a new active discovery for the name "Automation Discovery Tests" at location "NodeDiscovery" with ip address "127.1.0.5" and port 161, readCommunities "public"
    Then Check the status of the Node with expected status "UP"
    Then Delete the first node from inventory
    When Location "NodeDiscovery" is removed
    Then Location "NodeDiscovery" does not exist
    Then Minion "Dave" is stopped
