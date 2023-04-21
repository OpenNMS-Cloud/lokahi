@portal
Feature: Assigned admin can authorize in the instance

  Background: Scenario: IT Administrator adds BTO instance with name and self as admin
    Given BTO instance name "random" created
    And the IT Administrator sees an instance "random" in the list
    And click on 'Log in' button for the instance

  Scenario: Not assigned user cannot authorize in the instance
    Then set email address as "OKTA_USER"
    And click on 'Next' button
    And set password
    And click on 'Sign in' button
    But see 'We are sorry...' error with access restriction for "OKTA_USER" user

  Scenario: Assigned user can authorize in the instance
    Then set email address as "ADMIN"
    And click on 'Next' button
    And set password
    And click on 'Sign in' button
    And user sees the navigation panel for instance
