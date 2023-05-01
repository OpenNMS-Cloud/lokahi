@portal
Feature: Administrator can see instances in the table

  @TestCaseKey=CLOUD-T476
  Scenario: IT Administrator of a BTO Organization with no BTO instances sees empty table in BTO Summary page
    Given No BTO instances created
    Then sees 'No instances available.'

  @TestCaseKey=CLOUD-T476_1 @CLOUD-3243
  Scenario: IT Administrator of a BTO Organization can create a new instance from 'No instances available.' notification
    Given No BTO instances created
    Then sees 'No instances available.'
    When click on 'ADD INSTANCE' button
    Then the IT Administrator fills "CLOUD-T476_1" in 'Instance name'
    And  and selects 'Me' as option for 'Assign this instance to:'
    And the IT Administrator clicks on 'ADD INSTANCE' button
    Then the IT Administrator is brought back to the OpenNMS Cloud page
    And the IT Administrator gets success snackbar message "Cloud instance created."
    And the IT Administrator sees an instance "CLOUD-T476_1" in the list
    Then the IT Administrator opens 'Details' for the instance
    And search 'Search for user' input is empty
    And the IT Administrator sees "ADMIN" as a single user for the instance

  @TestCaseKey=CLOUD-T477
  Scenario: IT Administrator of a BTO Organization with BTO instances sees tables with all instances in BTO Summary page
    Given A list of BTO instances are created
      | A-Instance |
      | Z-Instance |
      | P-Instance |
      | D-Instance |
      | O-Instance |
      | G-Instance |
      | F-Instance |
      | E-Instance |
      | X-Instance |
      | K-Instance |
    Then set "Instance" in the 'Search Instance Name' field
    And see "none" sorting icon for 'Instance name'
    And  the IT Administrator sees a list of instances in the list
      | Z-Instance |
      | X-Instance |
      | P-Instance |
      | O-Instance |
      | K-Instance |
      | G-Instance |
      | F-Instance |
      | E-Instance |
      | D-Instance |
      | A-Instance |
    Then click on 'Instance Name' title to change sorting
    And see "ascending" sorting icon for 'Instance name'
    And  the IT Administrator sees a list of instances in the list
      | A-Instance |
      | D-Instance |
      | E-Instance |
      | F-Instance |
      | G-Instance |
      | K-Instance |
      | O-Instance |
      | P-Instance |
      | X-Instance |
      | Z-Instance |
    Then click on 'Instance Name' title to change sorting
    And see "descending" sorting icon for 'Instance name'
    And  the IT Administrator sees a list of instances in the list
      | Z-Instance |
      | X-Instance |
      | P-Instance |
      | O-Instance |
      | K-Instance |
      | G-Instance |
      | F-Instance |
      | E-Instance |
      | D-Instance |
      | A-Instance |


  @TestCaseKey=CLOUD-T478
  Scenario: IT Administrator of a BTO Organization with BTO instances can search instances - no match
    Given BTO instance name "CLOUD-T478" created
    When set "FAKE" in the 'Search Instance Name' field
    Then sees 'No results found'
    Then click on 'CLEAR SEARCH' button
    And 'Search Instance Name' field is empty
    And the IT Administrator sees a list of instances in the list
      | CLOUD-T478 |

  @TestCaseKey=CLOUD-T479
  Scenario: IT Administrator of a BTO Organization with BTO instances can search instances - partial match
    Given A list of BTO instances are created
      | CLOUD-middle-T479 |
      | prefix-CLOUD-T479 |
      | CLOUD-T479        |
      | CLOUD-T479-suffix |
    Then set "CLOUD-T479" in the 'Search Instance Name' field
    And the IT Administrator sees a list of instances in the list
      | prefix-CLOUD-T479 |
      | CLOUD-T479-suffix |
      | CLOUD-T479        |


  @TestCaseKey=CLOUD-T480
  Scenario: IT Administrator of a BTO Organization with BTO instances can search instances - more specific match
    Given A list of BTO instances are created
      | CLOUD-T480            |
      | CLOUD-T480-step       |
      | CLOUD-T480-step-final |
    Then set "CLOUD-T480" in the 'Search Instance Name' field
    And the IT Administrator sees a list of instances in the list
      | CLOUD-T480-step-final |
      | CLOUD-T480-step       |
      | CLOUD-T480            |
    Then add "-step" to the 'Search Instance Name' field
    And the IT Administrator sees a list of instances in the list
      | CLOUD-T480-step-final |
      | CLOUD-T480-step       |
    Then add "-final" to the 'Search Instance Name' field
    And the IT Administrator sees a list of instances in the list
      | CLOUD-T480-step-final |

  @TestCaseKey=CLOUD-T481
  Scenario: IT Administrator of a BTO Organization with BTO instances sees LOG IN button next to each instance in BTO Summary page
    Given A list of BTO instances are created
      | CLOUD-T481-1 |
      | CLOUD-T481-2 |
    Then set "CLOUD-T481" in the 'Search Instance Name' field
    And click on 'Log in' button for "CLOUD-T481-1" instance
    Then set email address as "ADMIN"
    And click on 'Next' button
    And set password
    And click on 'Sign in' button
    And user sees the navigation panel for instance
    Then verify the instance url for "CLOUD-T481-1" instance
    Then close the instance page
    And click on 'Log in' button for "CLOUD-T481-2" instance
    Then verify the instance url for "CLOUD-T481-2" instance

  @TestCaseKey=CLOUD-T482
  Scenario: IT Administrator of a BTO Organization with BTO instances sees "DETAILS" button next to each instance in BTO Summary page
    Given A list of BTO instances are created
      | CLOUD-T482-firstinstance  |
      | CLOUD-T482-secondinstance |
    Then set "CLOUD-T482" in the 'Search Instance Name' field
    And click on 'Details' button for "CLOUD-T482-firstinstance" instance
    And the IT Administrator sees the 'Cloud Instance Details' page for the "CLOUD-T482-firstinstance" instance
    Then click to 'go back' button to return to the OpenNMS Cloud page
    And click on 'Details' button for "CLOUD-T482-secondinstance" instance
    And the IT Administrator sees the 'Cloud Instance Details' page for the "CLOUD-T482-secondinstance" instance

  @TestCaseKey=CLOUD-T483
  Scenario: IT Administrator of a BTO Organization with BTO instances sees "url" information
    Given BTO instance name "CLOUD-T483" created
    And the IT Administrator sees an instance "CLOUD-T483" in the list
    And the IT Administrator opens 'Details' for the instance
    Then the IT Administrator sees the 'Cloud Instance Details' page for the "CLOUD-T483" instance
    And click on 'copy' button for URL
    And the IT Administrator gets success snackbar message "URL copied to clipboard."
    And has correct URL link in the clipboard that matches with the URL field

  @TestCaseKey=CLOUD-T497 @CLOUD-3253
  Scenario: IT Administrator of a BTO Organization with more than 10 BTO instances can navigate between pages
    Given A list of BTO instances are created
      | CLOUD-T497-1  |
      | CLOUD-T497-2  |
      | CLOUD-T497-3  |
      | CLOUD-T497-4  |
      | CLOUD-T497-5  |
      | CLOUD-T497-6  |
      | CLOUD-T497-7  |
      | CLOUD-T497-8  |
      | CLOUD-T497-9  |
      | CLOUD-T497-10 |
    And set "CLOUD-T497" in the 'Search Instance Name' field
    And user sees the pagination control panel for the "single" page
    Then BTO instance name "CLOUD-T497-11" created
    And set "" in the 'Search Instance Name' field
    And user sees the pagination control panel for the "first" page
    And the IT Administrator sees a list of instances in the list
      | CLOUD-T497-9  |
      | CLOUD-T497-8  |
      | CLOUD-T497-7  |
      | CLOUD-T497-6  |
      | CLOUD-T497-5  |
      | CLOUD-T497-4  |
      | CLOUD-T497-3  |
      | CLOUD-T497-2  |
      | CLOUD-T497-11 |
      | CLOUD-T497-10 |
    Then click on 'next page' button
    And user sees the pagination control panel for the "last" page
    And the IT Administrator sees a list of instances in the list
      | CLOUD-T497-1 |
    Then click on 'previous page' button
    And user sees the pagination control panel for the "first" page
    And the IT Administrator sees a list of instances in the list
      | CLOUD-T497-9  |
      | CLOUD-T497-8  |
      | CLOUD-T497-7  |
      | CLOUD-T497-6  |
      | CLOUD-T497-5  |
      | CLOUD-T497-4  |
      | CLOUD-T497-3  |
      | CLOUD-T497-2  |
      | CLOUD-T497-11 |
      | CLOUD-T497-10 |
