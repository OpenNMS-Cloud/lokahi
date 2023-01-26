Feature: Notification Grpc Processing

  Background: Common Test Setup
    Given clean setup
    Given grpc setup

  Scenario: Populate config via grpc
    Given Integration key set to "abc" via grpc
    Then verify key is "abc"
    Then tear down grpc setup

  Scenario: Populate config twice via grpc
    Given Integration key set to "abc" then "abcd" via grpc
    Then verify key is "abcd"
    Then tear down grpc setup
