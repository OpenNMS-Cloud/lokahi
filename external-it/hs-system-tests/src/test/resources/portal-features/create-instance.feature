@portal
Feature: Administrator can create a new instance

  @TestCaseKey=CLOUD-T484 @CLOUD-3243
  Scenario: IT Administrator adds cloud instance with name and self as admin
    Given clicks on '+ADD INSTANCE' button
    When fills "CLOUD-T484" in 'Instance name'
    Then and selects 'Me' as option for 'Assign this instance to:'
    And clicks on 'ADD INSTANCE' button
    Then is brought back to the OpenNMS Cloud page
    And gets success snackbar message "Cloud instance created."
    And sees an instance "CLOUD-T484" in the list
    Then click on 'Details' for the first instance
    And sees "ADMIN" as a single user for the instance

  @TestCaseKey=CLOUD-T487 @CLOUD-3243
  Scenario: IT Administrator adds cloud instance with name and assign it on another email
    Given clicks on '+ADD INSTANCE' button
    When fills "CLOUD-T487" in 'Instance name'
    Then and selects 'Someone else' as option for 'Assign this instance to:'
    And set assigned user email as "OKTA_USER"
    And clicks on 'ADD INSTANCE' button
    Then is brought back to the OpenNMS Cloud page
    And gets success snackbar message "Cloud instance created."
    And sees an instance "CLOUD-T487" in the list
    Then click on 'Details' for the first instance
    And sees "OKTA_USER" as a single user for the instance

  @TestCaseKey=CLOUD-T489
  Scenario: IT Administrator cancels adding a cloud instance
    Given clicks on '+ADD INSTANCE' button
    When clicks on 'X' button to close popup
    Then is brought back to the OpenNMS Cloud page

  @TestCaseKey=CLOUD-T490
  Scenario Outline: IT Administrator of a cloud Organization cannot add cloud instance with name containing characters outside of A-Z,a-z,0-9,-,_, ,
    Given clicks on '+ADD INSTANCE' button
    When fills <invalid_name> in 'Instance name'
    Then clicks on 'ADD INSTANCE' button
    Then an error message "Only letters, numbers, -, _ are allowed." appears for 'Instance name' field
    When fills <valid_name> in 'Instance name'
    Then the 'error message' for 'Instance name' is no longer displayed

    Examples:
      | valid_name              | invalid_name                                |
      | "Instance_1"            | "Instance!"                                 |
      | "Instance1234 for John" | "Instance1234 for 'John'"                   |
      | "Instance-1234"         | "Instance-1234<script>alert('1');</script>" |

  @TestCaseKey=CLOUD-T491
  Scenario: IT Administrator of a cloud Organization cannot add cloud instance without a name
    Given clicks on '+ADD INSTANCE' button
    When fills "" in 'Instance name'
    Then and selects 'Me' as option for 'Assign this instance to:'
    And clicks on 'ADD INSTANCE' button
    Then an error message "Name is required and can contain only letters and the \"-' .\" characters." appears for 'Instance name' field

  @TestCaseKey=CLOUD-T491_2
  Scenario: IT Administrator of a cloud Organization cannot add cloud instance without a name
    Given clicks on '+ADD INSTANCE' button
    When fills "   " in 'Instance name'
    Then and selects 'Me' as option for 'Assign this instance to:'
    And clicks on 'ADD INSTANCE' button
    But 'Add a Cloud instance' is visible
    And gets error snackbar message "Invalid name; must not be blank."

  @TestCaseKey=CLOUD-T492
  Scenario: IT Administrator of a cloud Organization can add cloud instance after correcting name with invalid characters
    Given clicks on '+ADD INSTANCE' button
    When fills "CL()UD-T492!" in 'Instance name'
    Then clicks on 'ADD INSTANCE' button
    Then an error message "Only letters, numbers, -, _ are allowed." appears for 'Instance name' field
    When fills "CLOUD-T492" in 'Instance name'
    And clicks on 'ADD INSTANCE' button
    Then is brought back to the OpenNMS Cloud page
    And gets success snackbar message "Cloud instance created."
    And sees an instance "CLOUD-T492" in the list

  @TestCaseKey=CLOUD-T493
  Scenario: IT Administrator of a cloud Organization cannot add cloud instance with a duplicate name
    Given clicks on '+ADD INSTANCE' button
    When fills "CLOUD-T493" in 'Instance name'
    Then and selects 'Me' as option for 'Assign this instance to:'
    And clicks on 'ADD INSTANCE' button
    And is brought back to the OpenNMS Cloud page
    Then  clicks on '+ADD INSTANCE' button
    And fills "CLOUD-T493" in 'Instance name'
    And and selects 'Me' as option for 'Assign this instance to:'
    And clicks on 'ADD INSTANCE' button
    Then gets error snackbar message "Instance with same name exists."

  @TestCaseKey=CLOUD-T494
  Scenario: IT Administrator cannot add cloud instance with name and username with invalid email
    Given clicks on '+ADD INSTANCE' button
    When fills "CLOUD-T494" in 'Instance name'
    Then and selects 'Someone else' as option for 'Assign this instance to:'
    And set assigned user email as "email@domain"
    And clicks on 'ADD INSTANCE' button
    But an error message "Invalid email format." appears for 'Email address' field
    Then set assigned user email as "OKTA_USER"
    And the 'error message' for 'Email address' is no longer displayed
    And clicks on 'ADD INSTANCE' button
    Then is brought back to the OpenNMS Cloud page
    And gets success snackbar message "Cloud instance created."
    And sees an instance "CLOUD-T494" in the list
    Then click on 'Details' for the first instance
    And sees "OKTA_USER" as a single user for the instance
