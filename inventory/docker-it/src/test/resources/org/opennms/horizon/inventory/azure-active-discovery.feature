Feature: Azure Active Discovery

  Background: Common Test Setup
    Given [Azure] External GRPC Port in system property "application-external-grpc-port"
    Given [Azure] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [Azure] Grpc TenantId "tenant-stream"
    Given [Azure] Create Grpc Connection for Inventory

  Scenario: Create new azure active discovery
    Given Azure Test Active Discovery
    When A GRPC request to create azure active discovery
    And A GRPC request to get tags for azure active discovery
    Then The response should assert for relevant fields
