Feature: Azure Active Discovery

  Background: Common Test Setup
    Given [Azure] External GRPC Port in system property "application-external-grpc-port"
    Given [Azure] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [Azure] Grpc TenantId "tenant-stream"
    Given [Azure] Grpc location "test-location"
    Given [Azure] Create Grpc Connection for Inventory
    Given [Common] Create "Default" Location

  Scenario: Create azure active discovery without tenant id
    Given Clear tenant id
    Given Azure Test Active Discovery
    When A GRPC request to create azure active discovery with exception expected
    Then verify exception "StatusRuntimeException" thrown with message "UNAUTHENTICATED: Invalid access token"

  Scenario: Create new azure active discovery
    Given Azure Test Active Discovery
    When A GRPC request to create azure active discovery
    And A GRPC request to get tags for azure active discovery
    Then The response should assert for relevant fields

  Scenario: Create duplicate azure active discovery
    Given Azure Test Active Discovery
    When A GRPC request to create azure active discovery with exception expected
    Then verify exception "StatusRuntimeException" thrown with message "INTERNAL: Azure Discovery already exists with the provided subscription, directory and client ID"
