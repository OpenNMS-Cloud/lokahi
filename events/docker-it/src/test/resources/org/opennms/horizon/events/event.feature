@Event
Feature: Event Service Basic Functionality

  Background: Common Test Setup
    Given [Event] External GRPC Port in system property "application-external-grpc-port"
    Given [Event] Grpc TenantId "event-tenant-stream"
    Given [Event] Create Grpc Connection for Events

    Scenario: Get event numbers
      Then verify there are 0 events

    Scenario:
      Given Initialize Trap Producer With Topic "events"
      When Send Trap Data to Kafka Listener via Producer with TenantId "event-tenant-stream" and LocationId "2"
      Then Check If There are 0 Events with NodeId 1 and Location "jahanzeb-loc"
