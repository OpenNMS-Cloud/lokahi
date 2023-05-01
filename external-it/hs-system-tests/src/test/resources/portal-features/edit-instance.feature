@portal
Feature: Administrator can edit an instance

  @TestCaseKey=CLOUD-T560
  Scenario: IT administrator can rename BTO instance
    Given BTO instance name "CLOUD-T560" created
    And the IT Administrator sees an instance "CLOUD-T560" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    Then set new name "CLOUD-T560-EDITED" in 'Instance Name' field
    And click on 'UPDATE' button
    Then 'Edit Instance' popup disappear
    And the IT Administrator sees the 'Cloud Instance Details' page for the "CLOUD-T560-EDITED" instance

  @TestCaseKey=CLOUD-T561
  Scenario: IT administrator cannot rename BTO instance to duplicate name
    Given BTO instance name "CLOUD-T561_one" created
    And BTO instance name "CLOUD-T561_two" created
    And the IT Administrator sees an instance "CLOUD-T561_one" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "CLOUD-T561_two" in 'Instance Name' field
    And click on 'UPDATE' button
    Then 'Edit Instance' popup disappear
    And the IT Administrator sees the 'Cloud Instance Details' page for the "CLOUD-T561_one" instance
    But the IT Administrator gets error snackbar message "Instance with same name exists."

  @TestCaseKey=CLOUD-T562
  Scenario: IT administrator cannot rename BTO instance to name outside character set
    Given BTO instance name "CLOUD-T562" created
    And the IT Administrator sees an instance "CLOUD-T562" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "INSTANT!" in 'Instance Name' field
    But 'Edit Instance' popup shows an error message "Only letters, numbers, -, _ are allowed." for Instance name

  @TestCaseKey=CLOUD-T562_2
  Scenario: IT administrator cannot rename BTO instance to empty name
    Given BTO instance name "CLOUD-T562_2" created
    And the IT Administrator sees an instance "CLOUD-T562_2" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "" in 'Instance Name' field
    But 'Edit Instance' popup shows an error message "Name is required and can contain only letters and the \"-' .\" characters" for Instance name

  @TestCaseKey=CLOUD-T562_3
  Scenario: IT administrator cannot rename BTO instance to name with only spaces
    Given BTO instance name "CLOUD-T562_3" created
    And the IT Administrator sees an instance "CLOUD-T562_3" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "   " in 'Instance Name' field
    And click on 'UPDATE' button
    Then the IT Administrator sees the 'Cloud Instance Details' page for the "CLOUD-T562_3" instance
    And the IT Administrator gets error snackbar message "Invalid name; must not be blank."

  @TestCaseKey=CLOUD-T563
  Scenario: IT administrator cancels renaming BTO instance with CANCEL
    Given BTO instance name "CLOUD-T563" created
    And the IT Administrator sees an instance "CLOUD-T563" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "SOME_NAME" in 'Instance Name' field
    And click on 'CANCEL' button to close 'Edit Instance' popup
    Then the IT Administrator sees the 'Cloud Instance Details' page for the "CLOUD-T563" instance
    And the IT Administrator doesn't see a snackbar message

  @TestCaseKey=CLOUD-T565
  Scenario: IT administrator cancels renaming BTO instance with X button
    Given BTO instance name "CLOUD-T565" created
    And the IT Administrator sees an instance "CLOUD-T565" in the list
    Then the IT Administrator opens 'Details' for the instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "SOME_NAME" in 'Instance Name' field
    And click on 'X' button to close 'Edit Instance' popup
    Then the IT Administrator sees the 'Cloud Instance Details' page for the "CLOUD-T565" instance
    And the IT Administrator doesn't see a snackbar message
