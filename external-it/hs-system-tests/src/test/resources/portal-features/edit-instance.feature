@portal
Feature: Administrator can edit an instance

  @TestCaseKey=CLOUD-T560
  Scenario: IT administrator can rename cloud instance
    Given cloud instance named "CLOUD-T560" is created
    And sees an instance "CLOUD-T560" in the list
    Then click on 'Details' for the first instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    Then set new name "CLOUD-T560-EDITED" in 'Instance Name' field
    And click on 'UPDATE' button
    Then 'Edit Instance' popup disappear
    And sees the 'Cloud Instance Details' page for the "CLOUD-T560-EDITED" instance

  @TestCaseKey=CLOUD-T561
  Scenario: IT administrator cannot rename cloud instance to duplicate name
    Given cloud instance named "CLOUD-T561_one" is created
    And cloud instance named "CLOUD-T561_two" is created
    And sees an instance "CLOUD-T561_one" in the list
    Then click on 'Details' for the first instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "CLOUD-T561_two" in 'Instance Name' field
    And click on 'UPDATE' button
    Then 'Edit Instance' popup disappear
    And sees the 'Cloud Instance Details' page for the "CLOUD-T561_one" instance
    But gets error snackbar message "Instance with same name exists."

  @TestCaseKey=CLOUD-T562
  Scenario: IT administrator cannot rename cloud instance to name outside character set
    Given cloud instance named "CLOUD-T562" is created
    And sees an instance "CLOUD-T562" in the list
    Then click on 'Details' for the first instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "INSTANT!" in 'Instance Name' field
    But 'Edit Instance' popup shows an error message "Only letters, numbers, -, _ are allowed." for Instance name

  @TestCaseKey=CLOUD-T562_2
  Scenario: IT administrator cannot rename cloud instance to empty name
    Given cloud instance named "CLOUD-T562_2" is created
    And sees an instance "CLOUD-T562_2" in the list
    Then click on 'Details' for the first instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "" in 'Instance Name' field
    But 'Edit Instance' popup shows an error message "Name is required and can contain only letters and the \"-' .\" characters" for Instance name

  @TestCaseKey=CLOUD-T562_3
  Scenario: IT administrator cannot rename cloud instance to name with only spaces
    Given cloud instance named "CLOUD-T562_3" is created
    And sees an instance "CLOUD-T562_3" in the list
    Then click on 'Details' for the first instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "   " in 'Instance Name' field
    And click on 'UPDATE' button
    Then sees the 'Cloud Instance Details' page for the "CLOUD-T562_3" instance
    And gets error snackbar message "Invalid name; must not be blank."

  @TestCaseKey=CLOUD-T563
  Scenario: IT administrator cancels renaming cloud instance with CANCEL
    Given cloud instance named "CLOUD-T563" is created
    And sees an instance "CLOUD-T563" in the list
    Then click on 'Details' for the first instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "SOME_NAME" in 'Instance Name' field
    And click on 'CANCEL' button to close 'Edit Instance' popup
    Then sees the 'Cloud Instance Details' page for the "CLOUD-T563" instance
    And doesn't see a snackbar message

  @TestCaseKey=CLOUD-T565
  Scenario: IT administrator cancels renaming cloud instance with X button
    Given cloud instance named "CLOUD-T565" is created
    And sees an instance "CLOUD-T565" in the list
    Then click on 'Details' for the first instance
    And click on 'edit' for instance name
    Then 'Edit Instance' popup appears
    And set new name "SOME_NAME" in 'Instance Name' field
    And click on 'X' button to close 'Edit Instance' popup
    Then sees the 'Cloud Instance Details' page for the "CLOUD-T565" instance
    And doesn't see a snackbar message
