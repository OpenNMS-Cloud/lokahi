@portal
Feature: Administrator can create new instance

  @TestCaseKey=CLOUD-T484
  Scenario: IT Administrator adds BTO instance with name and self as admin
    Given a IT Administrator clicks on '+ADD INSTANCE' button
    When the IT Administrator fills 'CLOUD-T484' in 'Instance name'
    Then  and selects 'Me' as option for 'Assign this instance to:'
    And the IT Administrator clicks on 'ADD INSTANCE' button
    Then the IT Administrator is brought back to the OpenNMS Cloud page
    And the IT Administrator gets success snackbar message 'Cloud instance created.'
    And the IT Administrator sees an instance 'CLOUD-T484' in the list
    Then the IT Administrator opens 'Details' for the instance
    And the IT Administrator sees 'ADMIN' as a single user for the instance

  @TestCaseKey=CLOUD-T487
  Scenario: IT Administrator adds BTO instance with name and assign it on another email
    Given a IT Administrator clicks on '+ADD INSTANCE' button
    When the IT Administrator fills 'CLOUD-T487' in 'Instance name'
    Then and selects 'Someone else' as option for 'Assign this instance to:'
    And set assigned user email as 'OKTA_USER'
    And the IT Administrator clicks on 'ADD INSTANCE' button
    Then the IT Administrator is brought back to the OpenNMS Cloud page
    And the IT Administrator gets success snackbar message 'Cloud instance created.'
    And the IT Administrator sees an instance 'CLOUD-T487' in the list
    Then the IT Administrator opens 'Details' for the instance
    And the IT Administrator sees 'OKTA_USER' as a single user for the instance

  @TestCaseKey=CLOUD-T489
  Scenario: IT Administrator cancels adding a BTO instance
    Given a IT Administrator clicks on '+ADD INSTANCE' button
    When the IT Administrator clicks on 'X' button to close popup
    Then the IT Administrator is brought back to the OpenNMS Cloud page

  @TestCaseKey=CLOUD-T490
  Scenario Outline: IT Administrator of a BTO Organization cannot add BTO instance with name containing characters outside of A-Z,a-z,0-9,-,_, ,
    Given  a IT Administrator clicks on '+ADD INSTANCE' button
    When the IT Administrator fills <invalid_name> in 'Instance name'
    Then the IT Administrator clicks on 'ADD INSTANCE' button
    Then an error message 'Only letters, numbers, -, _ are allowed.' appears for 'Instance name' field
    When the IT Administrator fills <valid_name> in 'Instance name'
    Then the 'error message' for 'Instance name' is no longer displayed

    Examples:
      | valid_name              | invalid_name                                |
      | 'Instance_1'            | 'Instance!'                                 |
      | 'Instance1234 for Jhon' | 'Instance1234 for \'Jhon\''                 |
      | 'Instance-1234'         | 'Instance-1234<script>alert("1");</script>' |


  @TestCaseKey=CLOUD-T491
  Scenario: IT Administrator of a BTO Organization cannot add BTO instance without a name
    Given  a IT Administrator clicks on '+ADD INSTANCE' button
    When the IT Administrator fills '' in 'Instance name'
    Then and selects 'Me' as option for 'Assign this instance to:'
    And the IT Administrator clicks on 'ADD INSTANCE' button
    Then an error message 'Name is required and can contain only letters and the "-\' ." characters.' appears for 'Instance name' field

  @TestCaseKey=CLOUD-T492
  Scenario: IT Administrator of a BTO Organization can add BTO instance after correcting name with invalid characters
    Given  a IT Administrator clicks on '+ADD INSTANCE' button
    When the IT Administrator fills 'CL()UD-T492!' in 'Instance name'
    Then the IT Administrator clicks on 'ADD INSTANCE' button
    Then an error message 'Only letters, numbers, -, _ are allowed.' appears for 'Instance name' field
    When the IT Administrator fills 'CLOUD-T492' in 'Instance name'
    And the IT Administrator clicks on 'ADD INSTANCE' button
    Then the IT Administrator is brought back to the OpenNMS Cloud page
    And the IT Administrator gets success snackbar message 'Cloud instance created.'
    And the IT Administrator sees an instance 'CLOUD-T492' in the list

  @TestCaseKey=CLOUD-T493
  Scenario: IT Administrator of a BTO Organization cannot add BTO instance with a duplicate name
    Given  a IT Administrator clicks on '+ADD INSTANCE' button
    When the IT Administrator fills 'CLOUD-T493' in 'Instance name'
    Then and selects 'Me' as option for 'Assign this instance to:'
    And the IT Administrator clicks on 'ADD INSTANCE' button
    And the IT Administrator is brought back to the OpenNMS Cloud page
    Then  a IT Administrator clicks on '+ADD INSTANCE' button
    And the IT Administrator fills 'CLOUD-T493' in 'Instance name'
    And and selects 'Me' as option for 'Assign this instance to:'
    And the IT Administrator clicks on 'ADD INSTANCE' button
    Then the IT Administrator gets error snackbar message 'Instance with same name exists.'

  @TestCaseKey=CLOUD-T494
  Scenario: IT Administrator cannot add BTO instance with name and username with invalid email
    Given a IT Administrator clicks on '+ADD INSTANCE' button
    When the IT Administrator fills 'CLOUD-T494' in 'Instance name'
    Then and selects 'Someone else' as option for 'Assign this instance to:'
    And set assigned user email as 'email@domain'
    And the IT Administrator clicks on 'ADD INSTANCE' button
    But an error message 'Invalid email format.' appears for 'Email address' field
    Then set assigned user email as 'OKTA_USER'
    And the 'error message' for 'Email address' is no longer displayed
    And the IT Administrator clicks on 'ADD INSTANCE' button
    Then the IT Administrator is brought back to the OpenNMS Cloud page
    And the IT Administrator gets success snackbar message 'Cloud instance created.'
    And the IT Administrator sees an instance 'CLOUD-T494' in the list
    Then the IT Administrator opens 'Details' for the instance
    And the IT Administrator sees 'OKTA_USER' as a single user for the instance
