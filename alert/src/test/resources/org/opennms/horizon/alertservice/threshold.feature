Feature: Alert Service Thresholding Functionality
  Background: Configure base URLs
    Given Application base HTTP URL in system property "application.base-http-url"
    Given Application base gRPC URL in system property "application.base-grpc-url"
    Given Kafka bootstrap URL in system property "kafka.bootstrap-servers"
    Given Kafka event topic "events"
    Given Kafka alert topic "alerts"
    Given Tenant id "thresholdTenantA"
    Given Monitor policy name "thresholdTenantA-policy" and memo "thresholdTenantA policy"
    Given Policy Rule name "thresholdTenantA-rule" and componentType "NODE"
    And Trigger events data
      | trigger_event   | count | overtime | overtime_unit | severity | clear_event    |
      | SNMP_Link_Down  | 2     | 0        | MINUTE        | MAJOR    |                |
      | SNMP_Link_Up    | 1     | 0        | MINUTE        | CLEARED  | SNMP_Link_Down |
      | SNMP_Cold_Start | 3     | 10       | MINUTE        | MAJOR    |                |
      | SNMP_Warm_Start | 1     | 0        | MINUTE        | CLEARED  |                |
    And Create a new policy with give parameters
    Given Tenant id "thresholdTenantF"
    And Create a new policy with give parameters

    # delete ThresholdEvents more than 10 minutes old.
  Scenario: Verify when a thresholding event is received from Kafka, a new alert is only created on passing the threshold
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" with tenant "thresholdTenantA" with node 10
    Then Verify alert topic has 0 messages with tenant "thresholdTenantA"
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" with tenant "thresholdTenantA" with node 10
    Then Verify alert topic has 0 messages with tenant "thresholdTenantA"
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Cold_Start" with tenant "thresholdTenantA" with node 10
    Then List alerts for tenant "thresholdTenantA", with timeout 15000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
      | alerts[0].severity == MAJOR |
    Then Verify alert topic has 1 messages with tenant "thresholdTenantA"

  Scenario: Verify when a thresholding event with no time limit is received from Kafka, a new alert is only created on passing the threshold
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "thresholdTenantF" with node 10
    Then Verify alert topic has 0 messages with tenant "thresholdTenantF"
    Then Send event with UEI "uei.opennms.org/generic/traps/SNMP_Link_Down" with tenant "thresholdTenantF" with node 10
    Then List alerts for tenant "thresholdTenantF", with timeout 15000ms, until JSON response matches the following JSON path expressions
      | alerts.size() == 1 |
      | alerts[0].counter == 1 |
      | alerts[0].severity == MAJOR |
    Then Verify alert topic has 1 messages with tenant "thresholdTenantF"
