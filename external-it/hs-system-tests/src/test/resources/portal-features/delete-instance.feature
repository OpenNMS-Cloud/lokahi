@portal
Feature: Administrator can delete an instance

  @TestCaseKey=CLOUD-T521
  Scenario: IT Administrator cancels deleting a cloud instance using button
    Given cloud instance named "CLOUD-T521" is created
    And sees an instance "CLOUD-T521" in the list
    Then click on 'Details' for the first instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And click on 'CANCEL' button to close the popup
    And sees the 'Cloud Instance Details' page for the "CLOUD-T521" instance

  @TestCaseKey=CLOUD-T522
  Scenario: IT Administrator cancels deleting a cloud instance using X button
    Given cloud instance named "CLOUD-T522" is created
    And sees an instance "CLOUD-T522" in the list
    Then click on 'Details' for the first instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And click on 'X' button to close the popup
    And sees the 'Cloud Instance Details' page for the "CLOUD-T522" instance

  @TestCaseKey=CLOUD-T523
  Scenario: IT Administrator enters non matching name cloud instance in "DELETE INSTANCE" confirmation
    Given cloud instance named "CLOUD-T523" is created
    And sees an instance "CLOUD-T523" in the list
    Then click on 'Details' for the first instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And enters "cloud2" as an 'Instance Name'
    Then click on 'DELETE' button to confirm deletion
    But sees error message 'Instance name does not match.' for 'Instance Name' field

  @TestCaseKey=CLOUD-T525
  Scenario: IT Administrator enters cloud instance named with name subset in "DELETE INSTANCE" confirmation
    Given cloud instance named "CLOUD-T525 DELETE" is created
    And sees an instance "CLOUD-T525 DELETE" in the list
    Then click on 'Details' for the first instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And enters "CLOUD-T525" as an 'Instance Name'
    Then click on 'DELETE' button to confirm deletion
    But sees error message 'Instance name does not match.' for 'Instance Name' field

  @TestCaseKey=CLOUD-T526
  Scenario:IT Administrator deletes cloud instance after confirmation
    Given cloud instance named "CLOUD-T526" is created
    And sees an instance "CLOUD-T526" in the list
    Then click on 'Details' for the first instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And enters "CLOUD-T526" as an 'Instance Name'
    Then click on 'DELETE' button to confirm deletion
    And is brought back to the OpenNMS Cloud page
    And doesn't see an instance "CLOUD-T526" in the list

  @TestCaseKey=CLOUD-T530
  Scenario: IT Administrator enters non matching then matching cloud instance named in "DELETE INSTANCE" confirmation
    Given cloud instance named "CLOUD-T530" is created
    And sees an instance "CLOUD-T530" in the list
    Then click on 'Details' for the first instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And enters "SUDO" as an 'Instance Name'
    Then click on 'DELETE' button to confirm deletion
    But sees error message 'Instance name does not match.' for 'Instance Name' field
    Then enters "CLOUD-T530" as an 'Instance Name'
    And error message for 'Instance Name' field disappears
    And click on 'DELETE' button to confirm deletion
    And is brought back to the OpenNMS Cloud page
    And doesn't see an instance "CLOUD-T530" in the list

  @TestCaseKey=CLOUD-T533
  Scenario: IT Administrator can no longer login cloud instance after deletion
    Given cloud instance named "CLOUD-T533" is created
    And sees an instance "CLOUD-T533" in the list
    Then click on 'Details' for the first instance
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
    Given cloud instance named "CLOUD-T538" is created
    And sees an instance "CLOUD-T538" in the list
    Then click on 'Details' for the first instance
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And enters "cloud-t538" as an 'Instance Name'
    Then click on 'DELETE' button to confirm deletion
    But sees error message 'Instance name does not match.' for 'Instance Name' field

  @TestCaseKey=CLOUD-T629
  Scenario: IT Administrator cannot delete an instance by its previous name
    Given cloud instance named "CLOUD-T629" is created
    And sees an instance "CLOUD-T629" in the list
    Then click on 'Details' for the first instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "NEW-NAME" in 'Instance Name' field
    And click on 'UPDATE' button
    And 'Edit Instance' popup disappear
    And click on 'DELETE INSTANCE' button
    And 'Instance Delete Confirmation' popup appears
    And enters "CLOUD-T629" as an 'Instance Name'
    And click on 'DELETE' button to confirm deletion
    But sees error message 'Instance name does not match.' for 'Instance Name' field
    Then enters "NEW-NAME" as an 'Instance Name'
    And error message for 'Instance Name' field disappears
    And  click on 'DELETE' button to confirm deletion
    And is brought back to the OpenNMS Cloud page
    And doesn't see an instance "CLOUD-T629" in the list
    And doesn't see an instance "NEW-NAME" in the list
