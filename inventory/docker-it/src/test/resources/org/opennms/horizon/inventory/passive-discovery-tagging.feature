Feature: Passive Discovery Tagging

  Background: Common Test Setup
    Given [PassiveDiscovery] External GRPC Port in system property "application-external-grpc-port"
    Given [PassiveDiscovery] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [PassiveDiscovery] Grpc TenantId "tenant-stream"
    Given [PassiveDiscovery] Grpc location "test-location"
    Given [PassiveDiscovery] Create Grpc Connection for Inventory

  Scenario: Create new tags on passive discovery
    Given A new passive discovery
    When A GRPC request to create tags "tag1,tag2" for passive discovery
    Then The passive discovery tag response should contain only tags "tag1,tag2"

  Scenario: Create multiple tags on multiple passive discovery
    Given 2 new passive discovery
    When A GRPC request to create tags "tag1,tag2" for both passive discovery
    Then The passive discovery tag response should contain only tags "tag1,tag2"
    And Both passive discovery have the same tags of "tag1,tag2"

  Scenario: Get a list of tags for passive discovery
    Given A new passive discovery with tags "tag1,tag2"
    When A GRPC request to fetch tags for passive discovery
    Then The passive discovery tag response should contain only tags "tag1,tag2"

  Scenario: Get an empty list of tags for passive discovery
    Given A new passive discovery
    When A GRPC request to fetch tags for passive discovery
    Then The passive discovery tag response should contain an empty list of tags

  Scenario: Get a list of tags for passive discovery and name like provided search term
    Given A new passive discovery with tags "abc,bcd"
    When A GRPC request to fetch all tags for passive discovery with name like "cd"
    Then The passive discovery tag response should contain only tags "bcd"

  Scenario: Get an empty list of tags for passive discovery and name like provided search term
    Given A new passive discovery with tags "abc,bcd"
    When A GRPC request to fetch all tags for passive discovery with name like "xyz"
    Then The passive discovery tag response should contain an empty list of tags

  Scenario: Remove tags from passive discovery
    Given A new passive discovery with tags "tag1,tag2"
    When A GRPC request to remove tag "tag1" for passive discovery
    Then The passive discovery tag response should contain only tags "tag2"
