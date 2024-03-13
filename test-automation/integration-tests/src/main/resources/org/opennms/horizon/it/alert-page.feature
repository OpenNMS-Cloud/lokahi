@alert-page
Feature: Alert Page

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

  Scenario Outline: Generate SNMP traps and trigger alerts
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
      | policy              | tag                   | trigger-event-id | trigger-event-name | severity | policy-rule      | location            | system-id           | node-name      | discovery              |
      | alert-page-policy01 | alert-page-policy-tag | 1                | SNMP Cold Start    | MINOR    | ap-policy-rule01 | alert-page-location | alert-page-system01 | ap_snmp_node01 | alert-page-discovery01 |
      | alert-page-policy02 | alert-page-policy-tag | 2                | SNMP Warm Start    | MAJOR    | ap-policy-rule02 | alert-page-location | alert-page-system02 | ap_snmp_node02 | alert-page-discovery02 |
      | alert-page-policy03 | alert-page-policy-tag | 4                | SNMP Link Down     | CRITICAL | ap-policy-rule04 | alert-page-location | alert-page-system03 | ap_snmp_node03 | alert-page-discovery03 |

  @skip
  Scenario: As a user I can view active alerts
    Given I am on the Alerts page
    When I view the Alert list
    Then I will see a list filtered by Alerts with an Active state ordered by "time"

  @skip
  Scenario: As a user I can sort alerts by time
    Given I am on the Alerts page
    When I view the Alert list
    And I sort the list by "time"
    Then I will see a list filtered by Alerts with an Active state ordered by "time"

  Scenario: As a user I can sort alerts by severity
    Given I am on the Alerts page
    When I view the Alert list
    And I sort the list by "severity"
    Then I will see a list filtered by Alerts with an Active state ordered by "severity"

  @skip
  Scenario: As a user I can sort alerts by node
    Given I am on the Alerts page
    When I view the Alert list
    And I sort the list by "nodeLabel"
    Then I will see a list filtered by Alerts with an Active state ordered by "nodeLabel"

  Scenario Outline: As a user I can filter alerts by a time window
    Given I am on the Alerts page
    When I view the Alert list
    And I pick a time window of "<time-window>"
    Then I will see a list filtered by Alerts that occurred in the time window

    Examples:
      | time-window   |
      | TODAY         |
      | LAST_24_HOURS |
      | SEVEN_DAYS    |

  @skip
  Scenario: As a user I can filter alerts by severity "CRITICAL"
    Given I am on the Alerts page
    When I view the Alert list
    And I filter the list by "severity"
    Then I will see a list filtered by Alerts with an Active state ordered by "nodeLabel"

  @skip
  Scenario: As a user I can filter alerts by node
    Given I am on the Alerts page
    When I view the Alert list
    And I filter the list by "nodeLabel"
    Then I will see a list filtered by Alerts with an Active state ordered by "nodeLabel"

  Scenario: As a user I can clear active filters
    Given I am on the Alerts page
    And I have a filter toggled for alerts with severity "CRITICAL"
    When I choose to clear filters
    Then alerts from all severities will show

  Scenario: As a user I can clear an active alert
    Given I am on the Alerts page
    And I have an ACTIVE alert
    When I choose to clear the alert
    Then the status of the Alert will change to "CLEARED"

  Scenario: As a user I can acknowledge an active alert
    Given I am on the Alerts page
    And I have an UNACKNOWLEDGED alert
    When I choose to acknowledge the alert
    Then the Alert is acknowledged
