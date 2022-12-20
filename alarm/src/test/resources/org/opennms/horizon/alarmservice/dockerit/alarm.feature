# NOTE: using retries after sending messages to Kafka to avoid failures due to race condition on testing
Feature: Alarm Service Basic Functionality

  Background: Configure base URLs
    Given Application Base URL in system property "application.base-url"
    Given Kafka Rest Server URL in system property "kafka-rest.url"

  Scenario: Verify when an event is received from Kafka, a new Alarm is created
    Then Send Event message to Kafka at topic "events-proto" with alarm reduction key "alarm.reduction-key.010"
    Then Verify the HTTP response code is 200
    Then send GET request to application at path "/alarms/list", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | totalCount == 1 |

  Scenario: Verify when an event is received from Kafka, a new Alarm is created
    Then Send Event message to Kafka at topic "events-proto" with alarm reduction key "alarm.reduction-key.020"
    Then Verify the HTTP response code is 200
    Then send GET request to application at path "/alarms/list", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | totalCount == 2 |

  Scenario: Verify alarm can be deleted
    Then Send Event message to Kafka at topic "events-proto" with alarm reduction key "alarm.reduction-key.030"
    Then Verify the HTTP response code is 200
    Then send GET request to application at path "/alarms/list", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | totalCount == 3 |
    Then Remember alarm id
    Then Send DELETE request to application at path "/alarms/delete"
    Then Verify the HTTP response code is 200
    Then Send GET request to application at path "/alarms/list"
    Then DEBUG dump the response body
    Then parse the JSON response
    Then Verify JSON path expressions match
      | totalCount == 2 |

  Scenario: Verify alarm can be cleared
    Then Send Event message to Kafka at topic "events-proto" with alarm reduction key "alarm.reduction-key.040"
    Then Verify the HTTP response code is 200
    Then send GET request to application at path "/alarms/list", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | totalCount == 3 |
    Then Send POST request to clear alarm at path "/alarms/clear"
    Then Verify the HTTP response code is 200
    Then Send GET request to application at path "/alarms/list"
    Then Verify alarm was cleared
    Then Send POST request to clear alarm at path "/alarms/unclear"
    Then Verify the HTTP response code is 200
    Then Send GET request to application at path "/alarms/list"
    Then Verify alarm was uncleared

  Scenario: Verify alarm can be acknowledged and unacknowledged
    Then Send Event message to Kafka at topic "events-proto" with alarm reduction key "alarm.reduction-key.050"
    Then Verify the HTTP response code is 200
    Then send GET request to application at path "/alarms/list", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | totalCount == 4 |
    Then Send POST request to acknowledge alarm at path "/alarms/ack"
    Then Verify the HTTP response code is 200
    Then Send GET request to application at path "/alarms/list"
    Then Verify alarm was acknowledged
    Then Send POST request to unacknowledge alarm at path "/alarms/unAck"
    Then Verify the HTTP response code is 200
    Then Send GET request to application at path "/alarms/list"
    Then Verify alarm was unacknowledged

  Scenario: Verify alarm severity can be set and escalated
    Then Send Event message to Kafka at topic "events-proto" with alarm reduction key "alarm.reduction-key.060"
    Then Verify the HTTP response code is 200
    Then send GET request to application at path "/alarms/list", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | totalCount == 5 |
    Then Send POST request to set alarm severity at path "/alarms/severity"
    Then Verify the HTTP response code is 200
    Then Send GET request to application at path "/alarms/list"
#    Then Verify alarm was acknowledged
    Then Send POST request to escalate alarm severity at path "/alarms/escalate"
    Then Verify the HTTP response code is 200
    Then Send GET request to application at path "/alarms/list"
    Then Verify alarm severity was escalated

  Scenario: Verify alarm reduction for duplicate events
    # Generate an alarm
    Then Send Event message to Kafka at topic "events-proto" with alarm reduction key "alarm.reduction-key.070"
    Then Verify the HTTP response code is 200
    Then send GET request to application at path "/alarms/list", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | totalCount == 6        |
      | alarms[0].counter == 1 |

    # Generate a duplicate
    Then Send Event message to Kafka at topic "events-proto" with alarm reduction key "alarm.reduction-key.070"
    Then Verify the HTTP response code is 200
    Then send GET request to application at path "/alarms/list", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | totalCount == 6        |
      | alarms[5].counter == 2 |

  Scenario: Verify alarm memo can be updated and removed
    Then Send Event message to Kafka at topic "events-proto" with alarm reduction key "alarm.reduction-key.080"
    Then Verify the HTTP response code is 200
    Then send GET request to application at path "/alarms/list", with timeout 5000ms, until JSON response matches the following JSON path expressions
      | totalCount == 7 |
    Then Send PUT request to add memo at path "/alarms/memo"
    Then Verify the HTTP response code is 200
    Then Send GET request to application at path "/alarms/list"
    Then DEBUG dump the response body
    Then Remember alarm id
#    Then parse the JSON response
    Then Send DELETE request to remove memo at path "/alarms/removeMemo"
    Then Verify the HTTP response code is 200

