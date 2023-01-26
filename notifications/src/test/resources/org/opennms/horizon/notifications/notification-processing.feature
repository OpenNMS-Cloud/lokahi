Feature: Notification Processing

  Background: Common Test Setup
    Given clean setup

  Scenario: Populate config
    Given Integration key set to "abc"
    Then verify key is "abc"

  Scenario: Populate config twice
    Given Integration key set to "abc"
    Given Integration key set to "abcd"
    Then verify key is "abcd"

  Scenario: Post notification
    Given Integration key set to "abc"
    Given Alarm posted via service
    Then verify pager duty rest method is called

  Scenario: Try to post notification with no config
    Given Alarm posted via service with no config
    Then verify exception "NotificationConfigUninitializedException" thrown
