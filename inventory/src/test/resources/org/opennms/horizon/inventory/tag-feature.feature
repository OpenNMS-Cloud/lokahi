Feature: Tags

  Background: Common Test Setup
    Given [Tags] External GRPC Port in system property "application-external-grpc-port"
    Given [Tags] Kafka Bootstrap URL in system property "kafka.bootstrap-servers"
    Given [Tags] Grpc TenantId "tenant-stream"
    Given [Tags] Create Grpc Connection for Inventory

  Scenario: Get a list of tags for valid Node ID
    Given a new node with tags "tag1,tag2"
    When a GRPC Request with a parameter "1"
    Then the response should contain only tags "tag1,tag2"

