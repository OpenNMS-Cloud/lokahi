Feature: Hello World

  Scenario: Say Hello
    Given horizon stream server base url in environment variable "HORIZON_STREAM_BASE_URL"
    Given Keycloak server base url in environment variable "KEYCLOAK_BASE_URL"
    Given Keycloak realm in environment variable "KEYCLOAK_REALM"
    Given Keycloak username in environment variable "KEYCLOAK_USERNAME"
    Given Keycloak password in environment variable "KEYCLOAK_PASSWORD"
    Then login to Keycloak
    Then send GET request to horizon-stream at path "/events/count"
    Then verify HTTP response code = 200

  Scenario: Say Hello to Minions
    Given horizon stream server base url in environment variable "HORIZON_STREAM_BASE_URL"
    Given Keycloak server base url in environment variable "KEYCLOAK_BASE_URL"
    Given Keycloak realm in environment variable "KEYCLOAK_REALM"
    Given Keycloak username in environment variable "KEYCLOAK_USERNAME"
    Given Keycloak password in environment variable "KEYCLOAK_PASSWORD"
    Then login to Keycloak
    Then send GET request to horizon-stream at path "/minions"
    Then verify HTTP response code = 200
    Then verify response has Minion location = "Default"
