@Policy
Feature: Monitor policy gRPC Functionality

  Background: Configure base URLs
    Given Application base HTTP URL in system property "application.base-http-url"
    Given Application base gRPC URL in system property "application.base-grpc-url"
    Given Kafka bootstrap URL in system property "kafka.bootstrap-servers"
    Given Kafka event topic "events"
    Given Kafka alert topic "alerts"
    Given Kafka monitoring policy topic "monitoring-policy"
    Given Monitoring policy kafka consumer

  Scenario: The default monitoring policy should exist
    Given Tenant id "different-tenant"
    Then The default monitoring policy exist with name "default_policy" and tag "default" and all notification enabled
    Then Verify the default policy rule has name "default_rule" and component type "NODE"
    Then Verify the default monitoring policy has the following data
      | triggerEvent    | severity |
      | SNMP_Cold_Start | CRITICAL |
      | SNMP_Warm_Start | MAJOR    |

  Scenario: Verify alert can be created based on the default policy
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" with tenant "new-tenant" with node 10
    Then List alerts for tenant "new-tenant", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |

  Scenario: Create a monitor policy with SNMP Trap event rule
    Given Tenant id "test-tenant"
    Given Monitor policy name "test-policy" and memo "the test policy"
    Given Policy tags
      | SNMP Trap        |
      | Default location |
    Given Notify by email "true"
    Given Policy Rule name "snmp rule" and componentType "NODE"
    Given Trigger events data
      | trigger_event   | count | overtime | overtime_unit | severity | clear_event |
      | SNMP_Cold_Start | 1     | 3        | MINUTE        | MAJOR    |             |
    Then Create a new policy with give parameters
    Then Verify the new policy has been created
    Then List policy should contain 1
    Then Verify monitoring policy for tenant "test-tenant" is sent to Kafka
