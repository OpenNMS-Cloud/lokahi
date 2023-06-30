@discovery
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

  Scenario: Discover a single node by it's IP
    Given No Minion running with location "SingleDiscovery"
    Given Location "SingleDiscovery" does not exist
    When Create location "SingleDiscovery"
    Then Location "SingleDiscovery" do exist
    Then Request certificate for location "SingleDiscovery"
    When Minion "test-minion" is started in location "SingleDiscovery"
    Given At least one Minion is running with location "SingleDiscovery"
    Then Node "node1" is started
    Then Discover "SingleDiscovery" for node "node1", location "SingleDiscovery" is created to discover by IP
    Then Check the status of the Node with expected status "UP"
    When Location "SingleDiscovery" is removed
    Then Location "SingleDiscovery" does not exist
    Then Minion "test-minion" is stopped

  Scenario: Discover a node using non-default snmp configuration
    Given No Minion running with location "NonDefaultSNMPDiscovery"
    Given Location "NonDefaultSNMPDiscovery" does not exist
    When Create location "NonDefaultSNMPDiscovery"
    Then Location "NonDefaultSNMPDiscovery" do exist
    Then Request certificate for location "NonDefaultSNMPDiscovery"
    When Minion "test-minion" is started in location "NonDefaultSNMPDiscovery"
    Given At least one Minion is running with location "NonDefaultSNMPDiscovery"
    Then Node "node1" is started with nondefault port and community
    Then Discover "NonDefaultSNMPDiscovery" for node "node1", location "NonDefaultSNMPDiscovery", port 2661, community "myCommunityString"
    Then Check the status of the Node with expected status "UP"
    When Location "NonDefaultSNMPDiscovery" is removed
    Then Location "NonDefaultSNMPDiscovery" does not exist
    Then Minion "test-minion" is stopped

  # Discovery with subnet doesn't appear to work...
#  Scenario: Discover multiple nodes through subnet scan
#    Given No Minion running with location "External"
#    Given Location "External" does not exist
#    When Create location "External"
#    Then Location "External" do exist
#    Then Request certificate for location "External"
#    When Minion "test-minion" is started in location "External"
#    Given At least one Minion is running with location "External"
#    # Then Wait for at least one minion for the given location reported by inventory with timeout 180000ms
#    Then Node "node1" is started
#    Then Node "node2" is started
#    Then Node "node3" is started
#    Then Subnet discovery "SubnetDiscovery" for nodes using location "External" and mask 28
#    Then Check the status of all 3 nodes with expected status "UP"
#    When Location "External" is removed
#    Then Location "External" does not exist
#    Then Minion "test-minion" is stopped

  Scenario: Discover multiple nodes using IP range
    Given No Minion running with location "RangeDiscovery"
    Given Location "RangeDiscovery" does not exist
    When Create location "RangeDiscovery"
    Then Location "RangeDiscovery" do exist
    Then Request certificate for location "RangeDiscovery"
    When Minion "test-minion" is started in location "RangeDiscovery"
    Given At least one Minion is running with location "RangeDiscovery"
    Then Node "node1" is started
    Then Node "node2" is started
    Then Node "node3" is started
    Then Subnet discovery "SubnetDiscovery" for nodes using location "RangeDiscovery" and IP range
    Then Check the status of all 3 nodes with expected status "UP"
    When Location "RangeDiscovery" is removed
    Then Location "RangeDiscovery" does not exist
    Then Minion "test-minion" is stopped

  # Discovery using a ',' between IPs does not currently work
#  Scenario: Discover multiple nodes using IP list
#    Given No Minion running with location "ListDiscovery"
#    Given Location "ListDiscovery" does not exist
#    When Create location "ListDiscovery"
#    Then Location "ListDiscovery" do exist
#    Then Request certificate for location "ListDiscovery"
#    When Minion "test-minion" is started in location "ListDiscovery"
#    Given At least one Minion is running with location "ListDiscovery"
#    Then Node "node1" is started
#    Then Node "node2" is started
#    Then Node "node3" is started
#    Then Subnet discovery "ListDiscovery" for nodes using location "ListDiscovery" and IP list
#    Then Check the status of all 3 nodes with expected status "UP"
#    When Location "ListDiscovery" is removed
#    Then Location "ListDiscovery" does not exist
#    Then Minion "test-minion" is stopped

