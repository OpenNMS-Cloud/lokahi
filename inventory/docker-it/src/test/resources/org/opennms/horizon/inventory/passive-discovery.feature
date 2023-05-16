Feature: Passive Discovery

  Background: Common Test Setup
    Given [Passive] External GRPC Port in system property "application-external-grpc-port"
    Given [Passive] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [Passive] Grpc TenantId "tenant-stream"
    Given [Passive] Create Grpc Connection for Inventory

  Scenario: Create and fetch passive discovery list
    Given Passive Discovery cleared
    Given Passive Discovery fields to persist
    When A GRPC request to upsert a passive discovery
    And A GRPC request to get passive discovery list
    And A GRPC request to get tags for passive discovery
    Then The upserted and the get of passive discovery should be the same
    Then the tags for passive discovery match what it was created with

  Scenario: Update and fetch passive discovery list
    Given Passive Discovery cleared
    Given Passive Discovery fields to persist
    When A GRPC request to upsert a passive discovery
    And A GRPC request to get passive discovery list
    Then The upserted and the get of passive discovery should be the same
    Given Passive Discovery fields to update
    When A GRPC request to upsert a passive discovery
    And A GRPC request to get passive discovery list
    Then The upserted and the get of passive discovery should be the same

  Scenario: Toggle passive discovery
    Given Passive Discovery cleared
    Given Passive Discovery fields to persist
    When A GRPC request to upsert a passive discovery
    And A GRPC request to get passive discovery list
    Then The upserted and the get of passive discovery should be the same
    Given A GRPC request to toggle a passive discovery
    And A GRPC request to get passive discovery list
    Then The passive discovery toggle should be false


