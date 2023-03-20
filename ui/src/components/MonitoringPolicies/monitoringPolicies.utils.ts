import {
  ThresholdCondition,
  EventColdRebootCondition,
  EventSNMPAuthFailureCondition,
  EventPortDownCondition
} from '@/types/policies'

// condition type guards
export const isThresholdCondition = (
  condition: ThresholdCondition | EventSNMPAuthFailureCondition | EventPortDownCondition | EventColdRebootCondition
): condition is ThresholdCondition => {
  return (condition as ThresholdCondition).level !== undefined
}

export const isEventColdRebootCondition = (
  condition: ThresholdCondition | EventSNMPAuthFailureCondition | EventPortDownCondition | EventColdRebootCondition
): condition is EventPortDownCondition => {
  return !isThresholdCondition(condition) && (condition as EventPortDownCondition).clearEvent !== undefined
}
