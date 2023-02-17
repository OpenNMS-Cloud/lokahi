Feature: Azure Discovery

  Background: Common Test Setup
    Given [Azure] External GRPC Port in system property "application-external-grpc-port"
    Given [Azure] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [Azure] Grpc TenantId "tenant-stream"
    Given [Azure] Create Grpc Connection for Inventory

  Scenario: Create new azure credentials
    Given Azure Test Credentials
    When A GRPC request to create azure credentials
    Then The response should assert for relevant fields
