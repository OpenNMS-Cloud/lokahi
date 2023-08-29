@monitoring-policy
Feature: Monitoring Policy

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

  Scenario Outline: Send trap and verify alert severity

    Given Policy rule name "<policy-rule>", component type "NODE" and event type "SNMP_TRAP"
    And Alert conditions data
      | trigger event id   | trigger event name   | event type | count | overtime | overtime unit | severity   | clear event |
      | <trigger-event-id> | <trigger-event-name> | SNMP_TRAP  | 1     | 0        |               | <severity> |             |
    When The user saves the monitoring policy with name "<policy>" and tag "<tag>"
    Then There is a monitoring policy with name "<policy>" and tag "<tag>"

    Given No minion running with location "<location>"
    Given Location "<location>" does not exist
    When Create location "<location>"
    Then Location "<location>" should exist
    Then User can retrieve a certificate for location "<location>"

    When Minion "<system-id>" is started at location "<location>"
    And SNMP node "<node-name>" is started in the network of minion "<system-id>"
    Then Discover "<discovery>" for snmp node "<node-name>", location "<location>" is created to discover by IP with policy tag "<tag>"

    When The snmp node sends a "<trigger-event-name>" to Minion
    Then An alert with "<trigger-event-name>" should be triggered with severity "<severity>"

    When Minion "<system-id>" is stopped
    And Location "<location>" is removed
    Then Location "<location>" does not exist

    Examples:
      | policy                   | tag                     | trigger-event-id | trigger-event-name | severity | policy-rule   | location                        | system-id                       | node-name      | discovery                          |
      | test-monitoring-policy01 | monitoring-policy-tag01 | 1                | SNMP Cold Start    | MINOR    | policy-rule01 | test-monitoring-policy-location | test-monitoring-policy-system01 | mp_snmp_node01 | test-monitoring-policy-discovery01 |
      | test-monitoring-policy02 | monitoring-policy-tag02 | 2                | SNMP Warm Start    | MAJOR    | policy-rule02 | test-monitoring-policy-location | test-monitoring-policy-system02 | mp_snmp_node02 | test-monitoring-policy-discovery02 |
      | test-monitoring-policy03 | monitoring-policy-tag03 | 4                | SNMP Link Down     | CRITICAL | policy-rule04 | test-monitoring-policy-location | test-monitoring-policy-system03 | mp_snmp_node03 | test-monitoring-policy-discovery03 |
