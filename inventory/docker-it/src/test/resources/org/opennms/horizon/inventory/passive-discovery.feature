Feature: Passive Discovery

  Background: Common Test Setup
    Given [Passive] External GRPC Port in system property "application-external-grpc-port"
    Given [Passive] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [Passive] Grpc TenantId "tenant-stream"
    Given [Passive] Create Grpc Connection for Inventory

  Scenario: Create and fetch passive discovery list
    Given Passive Discovery fields to persist
    When A GRPC request to create a new passive discovery
    And A GRPC request to get passive discovery list
    And A GRPC request to get tags for passive discovery
    Then The creation and the get of passive discovery should be the same
    Then the tags for passive discovery match what it was created with
