@passive-discovery
Feature: Passive Discovery

  Background: Common Test Setup
    Given [Passive] External GRPC Port in system property "application-external-grpc-port"
    Given [Passive] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [Passive] Grpc TenantId "tenant-stream"
    Given [Passive] Create Grpc Connection for Inventory
    Given [Common] Create "Testing Passive Discovery" Location
    Given Passive Discovery cleared

  Scenario: Create and fetch passive discovery list
    Given Passive Discovery fields to persist for location named "Testing Passive Discovery"
    When A GRPC request to upsert a passive discovery
    And A GRPC request to get passive discovery list
    And A GRPC request to get tags for passive discovery
    Then The upserted and the get of passive discovery should be the same
    Then the tags for passive discovery match what it was created with

  Scenario: Update and fetch passive discovery list
    Given Passive Discovery fields to persist for location named "Testing Passive Discovery"
    When A GRPC request to upsert a passive discovery
    And A GRPC request to get passive discovery list
    Then The upserted and the get of passive discovery should be the same
    Given Passive Discovery fields to update for location named "Testing Passive Discovery"
    When A GRPC request to upsert a passive discovery
    And A GRPC request to get passive discovery list
    Then The upserted and the get of passive discovery should be the same

  Scenario: Toggle passive discovery
    Given Passive Discovery fields to persist for location named "Testing Passive Discovery"
    When A GRPC request to upsert a passive discovery
    And A GRPC request to get passive discovery list
    Then The upserted and the get of passive discovery should be the same
    Given A GRPC request to toggle a passive discovery
    And A GRPC request to get passive discovery list
    Then The passive discovery toggle should be false

  Scenario: Send new suspect event and create node
    Given Passive Discovery fields to persist for location named "Testing Passive Discovery"
    When A GRPC request to upsert a passive discovery
    And A GRPC request to get passive discovery list
    Then The upserted and the get of passive discovery should be the same
    Given A GRPC request to enable a passive discovery
    When A new suspect event sent to Inventory for location named "Testing Passive Discovery" and IP Address "192.168.5.1"
    Then A new node should be created with location "Testing Passive Discovery" and IP Address "192.168.5.1"




