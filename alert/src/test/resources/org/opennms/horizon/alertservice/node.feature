@Node
Feature: Node operation feature

  Background: Configure base URLs
    Given Application base HTTP URL in system property "application.base-http-url"
    Given Application base gRPC URL in system property "application.base-grpc-url"
    Given Kafka bootstrap URL in system property "kafka.bootstrap-servers"
    Given Kafka node topic "node"
    Given Kafka event topic "events"
    Given Kafka alert topic "alerts"

  Scenario: Insert node when receive new message
    Given Tenant id "tenant-1"
    And A monitoring policy named "my-policy" with tag "tag1"
    And The policy has a rule named "my-rule" with component type "NODE" and trap definitions
      | trigger_event_name | count | overtime | overtime_unit | severity | clear_event_name |
      | SNMP Link Down     | 1     | 0        | MINUTE        | MINOR    |                  |
      | SNMP Link Up       | 1     | 0        | MINUTE        | CLEARED  | SNMP Link Down   |
      | SNMP Cold Start    | 1     | 0        | MINUTE        | MAJOR    |                  |
      | SNMP Warm Start    | 1     | 0        | MINUTE        | CRITICAL |                  |
    And The policy is created in the tenant
    Given [Node] operation data
      | id | tenant_id | label  |
      | 1  | tenant-1  | first  |
      | 2  | tenant-2  | second |
    And Sent node message to Kafka topic
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" on node 2
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 1
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 2                 |
      | alerts[0].label == SNMP Cold Start |
      | alerts[0].nodeName == BLANK        |
      | alerts[1].label == SNMP Link Down  |
      | alerts[1].nodeName == first        |
    Then List alerts for the tenant and label "first", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1                |
      | alerts[0].label == SNMP Link Down |
      | alerts[0].nodeName == first       |

  Scenario: Update node when receive new message
    Given Tenant id "tenant-3"
    And A monitoring policy named "my-policy" with tag "tag1"
    And The policy has a rule named "my-rule" with component type "NODE" and trap definitions
      | trigger_event_name | count | overtime | overtime_unit | severity | clear_event_name |
      | SNMP Link Down     | 1     | 0        | MINUTE        | MINOR    |                  |
      | SNMP Link Up       | 1     | 0        | MINUTE        | CLEARED  | SNMP Link Down   |
      | SNMP Cold Start    | 1     | 0        | MINUTE        | MAJOR    |                  |
      | SNMP Warm Start    | 1     | 0        | MINUTE        | CRITICAL |                  |
    And The policy is created in the tenant
    Given [Node] operation data
      | id | tenant_id | label  |
      | 1  | tenant-3  | first  |
      | 2  | tenant-2  | second |
    And Sent node message to Kafka topic
    Given [Node] operation data
      | id | tenant_id | label |
      | 1  | tenant-3  | third |
    And Sent node message to Kafka topic
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" on node 2
    Then An event is sent with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" on node 1
    Then List alerts for the tenant, until JSON response matches the following JSON path expressions
      | alerts.size() == 2                 |
      | alerts[0].label == SNMP Cold Start |
      | alerts[0].nodeName == BLANK        |
      | alerts[1].label == SNMP Link Down  |
      | alerts[1].nodeName == third        |
