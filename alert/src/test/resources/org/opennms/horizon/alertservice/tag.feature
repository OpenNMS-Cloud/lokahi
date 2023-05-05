@Tag
Feature: Tag operation feature

  Background: Configure base URLs
    Given Application base HTTP URL in system property "application.base-http-url"
    Given Application base gRPC URL in system property "application.base-grpc-url"
    Given Kafka bootstrap URL in system property "kafka.bootstrap-servers"
    Given Kafka tag topic "tag-operation"
    Given Kafka event topic "events"
    Given Kafka alert topic "alerts"

  Scenario: Insert tag when receive new message
    Given Tenant "tenant-1"
    Given Tag operation data
      | action     | name     | node_ids |
      | ASSIGN_TAG | test-tag | 100,200  |
    And Sent tag operation message to Kafka topic
    Then Verify list tag with size 1 and node ids
      | 100 | 200 |
    Given Tag operation data
      | action     | name     | node_ids |
      | ASSIGN_TAG | test-tag | 100,300  |
    And Sent tag operation message to Kafka topic
    Then Verify list tag with size 1 and node ids
      | 100 | 200 | 300 |
    Given Tag operation data
      | action     | name     | node_ids |
      | REMOVE_TAG | test-tag | 100,200  |
    And Sent tag operation message to Kafka topic
    Then Verify list tag with size 1 and node ids
      | 300 |
    Given Tag operation data
      | action     | name     | node_ids |
      | REMOVE_TAG | test-tag | 300      |
    And Sent tag operation message to Kafka topic
    Then Verify list tag is empty
