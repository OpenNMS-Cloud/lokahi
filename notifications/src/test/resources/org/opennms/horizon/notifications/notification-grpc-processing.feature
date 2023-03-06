Feature: Notification Grpc Processing

  Background: Common Test Setup
    Given clean setup
    Given grpc setup

  Scenario: Populate config via grpc
    Given Integration "test-tenant" key set to "abc" via grpc
    Then verify "test-tenant" key is "abc"
    Then verify "other-tenant" key is not set
    Then tear down grpc setup

  Scenario: Populate config twice via grpc
    Given Integration "test-tenant" key set to "abc" then "abcd" via grpc
    Then verify "test-tenant" key is "abcd"
    Then tear down grpc setup

  Scenario: Populate two different configs via grpc
    Given Integration "test-tenant" then "other-tenant" key set to "abc" then "abcd" via grpc
    Then verify "test-tenant" key is "abc"
    Then verify "other-tenant" key is "abcd"
    Then tear down grpc setup
