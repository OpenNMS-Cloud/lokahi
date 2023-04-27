@portal
Feature: Administrator can delete an instance

  @TestCaseKey=CLOUD-T521
  Scenario: IT Administrator cancels deleting a BTO instance using button
    Given BTO instance name "CLOUD-T521" created
    And the IT Administrator sees an instance "CLOUD-T521" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And click on 'CANCEL' button to close the popup
    And the IT Administrator sees the 'Cloud Instance Details' page for the "CLOUD-T521" instance

  @TestCaseKey=CLOUD-T522
  Scenario: IT Administrator cancels deleting a BTO instance using X button
    Given BTO instance name "CLOUD-T522" created
    And the IT Administrator sees an instance "CLOUD-T522" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And click on 'X' button to close the popup
    And the IT Administrator sees the 'Cloud Instance Details' page for the "CLOUD-T522" instance

  @TestCaseKey=CLOUD-T523
  Scenario: IT Administrator enters non matching name BTO instance in "DELETE INSTANCE" confirmation
    Given BTO instance name "CLOUD-T523" created
    And the IT Administrator sees an instance "CLOUD-T523" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And the IT Administrator enters "BTO2" as an 'Instance Name'
    Then click on 'DELETE' button to confirm deletion
    But sees error message 'Instance name does not match.' for 'Instance Name' field

  @TestCaseKey=CLOUD-T525
  Scenario: IT Administrator enters BTO instance name with name subset in "DELETE INSTANCE" confirmation
    Given BTO instance name "CLOUD-T525 DELETE" created
    And the IT Administrator sees an instance "CLOUD-T525 DELETE" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And the IT Administrator enters "CLOUD-T525" as an 'Instance Name'
    Then click on 'DELETE' button to confirm deletion
    But sees error message 'Instance name does not match.' for 'Instance Name' field

  @TestCaseKey=CLOUD-T526
  Scenario:IT Administrator deletes BTO instance after confirmation
    Given BTO instance name "CLOUD-T526" created
    And the IT Administrator sees an instance "CLOUD-T526" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And the IT Administrator enters "CLOUD-T526" as an 'Instance Name'
    Then click on 'DELETE' button to confirm deletion
    And the IT Administrator is brought back to the OpenNMS Cloud page
    And the IT Administrator doesn't see an instance "CLOUD-T526" in the list

  @TestCaseKey=CLOUD-T530
  Scenario: IT Administrator enters non matching then matching BTO instance name in "DELETE INSTANCE" confirmation
    Given BTO instance name "CLOUD-T530" created
    And the IT Administrator sees an instance "CLOUD-T530" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And the IT Administrator enters "SUDO" as an 'Instance Name'
    Then click on 'DELETE' button to confirm deletion
    But sees error message 'Instance name does not match.' for 'Instance Name' field
    Then the IT Administrator enters "CLOUD-T530" as an 'Instance Name'
    And error message for 'Instance Name' field disappears
    And  click on 'DELETE' button to confirm deletion
    And the IT Administrator is brought back to the OpenNMS Cloud page
    And the IT Administrator doesn't see an instance "CLOUD-T530" in the list

  @TestCaseKey=CLOUD-T533
  Scenario: IT Administrator can no longer login BTO instance after deletion
    Given BTO instance name "CLOUD-T533" created
    And the IT Administrator sees an instance "CLOUD-T533" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on the instance 'URL' link
    And someone deletes the 'CLOUD-T533' instance
    Then Cloud login page appears
    And set email address as "ADMIN"
    And click on 'Next' button
    And set password
    And click on 'Sign in' button
    But see 'We are sorry...' error with access restriction for "ADMIN" user

  @TestCaseKey=CLOUD-T538
  Scenario: IT Administrator cannot delete instance if the name case does not match
    Given BTO instance name "CLOUD-T538" created
    And the IT Administrator sees an instance "CLOUD-T538" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And the IT Administrator enters "cloud-t538" as an 'Instance Name'
    Then click on 'DELETE' button to confirm deletion
    But sees error message 'Instance name does not match.' for 'Instance Name' field

  @TestCaseKey=CLOUD-T629
  Scenario: IT Administrator cannot delete an instance by its previous name
    Given BTO instance name "CLOUD-T629" created
    And the IT Administrator sees an instance "CLOUD-T629" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "NEW-NAME" in 'Instance Name' field
    And click on 'UPDATE' button
    And 'Edit Instance' popup disappear
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And the IT Administrator enters "CLOUD-T629" as an 'Instance Name'
    And click on 'DELETE' button to confirm deletion
    But sees error message 'Instance name does not match.' for 'Instance Name' field
    Then the IT Administrator enters "NEW-NAME" as an 'Instance Name'
    And error message for 'Instance Name' field disappears
    And  click on 'DELETE' button to confirm deletion
    And the IT Administrator is brought back to the OpenNMS Cloud page
    And the IT Administrator doesn't see an instance "CLOUD-T629" in the list
    And the IT Administrator doesn't see an instance "NEW-NAME" in the list
