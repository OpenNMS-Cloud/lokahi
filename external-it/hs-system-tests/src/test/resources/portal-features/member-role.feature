@portal-member
Feature: IT Engineer with the MEMBER role can only see data in Portal

  @TestCaseKey=CLOUD-T637
  Scenario: IT Engineer cannot create a new instance by 'ADD INSTANCE' button when user isn't assigned to any instance (no instance).
    Given No cloud instances created
    But does not see the '+ADD INSTANCE' button

  @TestCaseKey=CLOUD-T638 @CLOUD-3250
  Scenario: IT Engineer cannot create a new instance by 'ADD INSTANCE' button from 'No instances available' section.
    Given No cloud instances created
    But sees 'No instances available.' section without 'ADD INSTANCE' button

  @TestCaseKey=CLOUD-T579
  Scenario: IT Engineer does not see OpenNMS Cloud subscription info
    Given No cloud instances created
    Then does not see subscription information

  @TestCaseKey=CLOUD-T581
  Scenario: IT Engineer cannot edit OpenNMS Cloud instance
    Given cloud instance named "CLOUD-T581" is assigned to "MEMBER" user
    And sees an instance "CLOUD-T581" in the list
    Then click on 'Details' for the first instance
    And sees the 'Cloud Instance Details' page for the "CLOUD-T581" instance
    But does not see the 'edit' button for the instance name

  @TestCaseKey=CLOUD-T582
  Scenario: IT Engineer cannot delete OpenNMS Cloud instance
    Given cloud instance named "CLOUD-T582" is assigned to "MEMBER" user
    And sees an instance "CLOUD-T582" in the list
    Then click on 'Details' for the first instance
    And sees the 'Cloud Instance Details' page for the "CLOUD-T582" instance
    But does not see the 'DELETE INSTANCE' button

  @TestCaseKey=CLOUD-T583
  Scenario: IT Engineer can add user to OpenNMS Cloud instance
    Given cloud instance named "CLOUD-T583" is assigned to "MEMBER" user
    And sees an instance "CLOUD-T583" in the list
    Then click on 'Details' for the first instance
    And sees the 'Cloud Instance Details' page for the "CLOUD-T583" instance
    And sees "MEMBER" as a single user for the instance
    Then click on 'ADD USER' button
    And 'Add user' popup appears
    And set user email as "OKTA_USER" in 'Add user' popup
    And confirms user addition by clicking on 'ADD USER' button
    And 'Add user' popup appears
    And sees following users as members of the instance
      | MEMBER    |
      | OKTA_USER |

  @TestCaseKey=CLOUD-T584
  Scenario: IT Engineer sees OpenNMS Cloud instance based on membership
    Given cloud instance named "CLOUD-T584-ADMIN" is assigned to "ADMIN" user
    And cloud instance named "CLOUD-T584-MEMBER" is assigned to "MEMBER" user
    And cloud instance named "CLOUD-T584-OKTA" is assigned to "OKTA_USER" user
    Then set "" in the 'Search Instance Name' field
    And sees a list of instances in the list
      | CLOUD-T584-MEMBER |

  @TestCaseKey=CLOUD-T585
  Scenario: IT Engineer will see new assigned instances when page is refreshed
    Given cloud instance named "CLOUD-T585" is assigned to "MEMBER" user
    And cloud instance named "CLOUD-T585-new" is created
    And set "CLOUD-T585" in the 'Search Instance Name' field
    And sees a list of instances in the list
      | CLOUD-T585 |
    Then "MEMBER" user was assigned to "CLOUD-T585" instance
    And refresh the page
    And sees a list of instances in the list
      | CLOUD-T585-new |
      | CLOUD-T585     |

  @TestCaseKey=CLOUD-T586 @CLOUD-3170
  Scenario: IT Engineer no longer sees OpenNMS Cloud instance after revoke
    Given cloud instance named "CLOUD-T586" is assigned to "MEMBER" user
    And cloud instance named "CLOUD-T586-revoked" is assigned to "MEMBER" user
    Then set "CLOUD-T586" in the 'Search Instance Name' field
    And sees a list of instances in the list
      | CLOUD-T586-revoked |
      | CLOUD-T586         |
    Then "MEMBER" user was revoked from "CLOUD-T586-revoked" instance
    Then refresh the page
    And sees a list of instances in the list
      | CLOUD-T586 |

  @TestCaseKey=CLOUD-T646
  Scenario: IT Engineer can log in to an assigned instance
    Given cloud instance named "CLOUD-T585" is assigned to "MEMBER" user
    And cloud instance named "CLOUD-T585-new" is created
    And sees an instance "CLOUD-T585" in the list
    And click on 'Log in' button for the instance
    Then Cloud login page appears
    And login to Cloud instance as "MEMBER" user
    And user sees the navigation panel for instance
