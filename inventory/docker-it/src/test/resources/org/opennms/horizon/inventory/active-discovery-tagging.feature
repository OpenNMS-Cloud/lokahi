Feature: Active Discovery Tagging

  Background: Common Test Setup
    Given [ActiveDiscovery] External GRPC Port in system property "application-external-grpc-port"
    Given [ActiveDiscovery] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [ActiveDiscovery] Grpc TenantId "tenant-stream"
    Given [ActiveDiscovery] Grpc location "test-location"
    Given [ActiveDiscovery] Create Grpc Connection for Inventory
    Given [Common] Create "location" Location

  Scenario: Create new tags on active discovery
    Given A new active discovery
    When A GRPC request to create tags "tag1,tag2" for active discovery
    Then The active discovery tag response should contain only tags "tag1,tag2"

  Scenario: Create multiple tags on multiple active discovery
    Given 2 new active discovery
    When A GRPC request to create tags "tag1,tag2" for both active discovery
    Then The active discovery tag response should contain only tags "tag1,tag2"
    And Both active discovery have the same tags of "tag1,tag2"

  Scenario: Get a list of tags for active discovery
    Given A new active discovery with tags "tag1,tag2"
    When A GRPC request to fetch tags for active discovery
    Then The active discovery tag response should contain only tags "tag1,tag2"

  Scenario: Get an empty list of tags for active discovery
    Given A new active discovery
    When A GRPC request to fetch tags for active discovery
    Then The active discovery tag response should contain an empty list of tags

  Scenario: Get a list of tags for active discovery and name like provided search term
    Given A new active discovery with tags "abc,bcd"
    When A GRPC request to fetch all tags for active discovery with name like "cd"
    Then The active discovery tag response should contain only tags "bcd"

  Scenario: Get an empty list of tags for active discovery and name like provided search term
    Given A new active discovery with tags "abc,bcd"
    When A GRPC request to fetch all tags for active discovery with name like "xyz"
    Then The active discovery tag response should contain an empty list of tags

  Scenario: Remove tags from active discovery
    Given A new active discovery with tags "tag1,tag2"
    When A GRPC request to remove tag "tag1" for active discovery
    Then The active discovery tag response should contain only tags "tag2"
