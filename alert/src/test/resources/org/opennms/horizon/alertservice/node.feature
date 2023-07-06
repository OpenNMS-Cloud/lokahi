@Node
Feature: Node operation feature

  Background: Configure base URLs
    Given Application base HTTP URL in system property "application.base-http-url"
    Given Application base gRPC URL in system property "application.base-grpc-url"
    Given Kafka bootstrap URL in system property "kafka.bootstrap-servers"
    Given Kafka node topic "node"
    Given Kafka event topic "events"
    Given Kafka alert topic "alerts"
    Given Tenant id "tenant-1"
    Given Monitor policy name "tenantA-policy" and memo "tenantA policy"
    Given Policy Rule name "tenantA-rule" and componentType "NODE"
    And Alert condition data
      | trigger_event_type | count | overtime | overtime_unit | severity | clear_event_type |
      | SNMP_Link_Down     | 1     | 0        | MINUTE        | MINOR    |                  |
      | SNMP_Link_Up       | 1     | 0        | MINUTE        | CLEARED  | SNMP_Link_Down   |
      | SNMP_Cold_Start    | 1     | 0        | MINUTE        | MAJOR    |                  |
      | SNMP_Warm_Start    | 1     | 0        | MINUTE        | CRITICAL |                  |
    And Create a new policy with give parameters
    Given Tenant id "tenant-3"
    And Create a new policy with give parameters

  Scenario: Insert node when receive new message
    Given [Node] Tenant "tenant-1"
    Given [Node] operation data
      | id  | tenant_id     | label    |
      | 1   | tenant-1      | first    |
      | 2   | tenant-2      | second   |
    And Sent node message to Kafka topic
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" with tenant "tenant-1" with node 2
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenant-1" with node 1
    Then List alerts for tenant "tenant-1", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 2                      |
      | alerts[0].label == SNMP_Cold_Start      |
      | alerts[0].nodeName == BLANK             |
      | alerts[1].label == SNMP_Link_Down       |
      | alerts[1].nodeName == first             |
    Then List alerts for tenant "tenant-1" and label "first", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1                      |
      | alerts[0].label == SNMP_Link_Down       |
      | alerts[0].nodeName == first             |

  Scenario: Update node when receive new message
    Given [Node] Tenant "tenant-3"
    Given [Node] operation data
      | id  | tenant_id     | label    |
      | 1   | tenant-3      | first    |
      | 2   | tenant-2      | second   |
    And Sent node message to Kafka topic
    Given [Node] operation data
      | id  | tenant_id     | label    |
      | 1   | tenant-3      | third    |
    And Sent node message to Kafka topic
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" with tenant "tenant-3" with node 2
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "tenant-3" with node 1
    Then List alerts for tenant "tenant-3", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 2                      |
      | alerts[0].label == SNMP_Cold_Start      |
      | alerts[0].nodeName == BLANK             |
      | alerts[1].label == SNMP_Link_Down       |
      | alerts[1].nodeName == third             |
