export enum DetectionMethodTypes {
  THRESHOLD = 'THRESHOLD',
  EVENT = 'EVENT'
}

export enum SNMPEventType {
  COLD_REBOOT = 'COLD_REBOOT',
  WARM_REBOOT = 'WARM_REBOOT',
  DEVICE_UNREACHABLE = 'DEVICE_UNREACHABLE',
  SNMP_AUTH_FAILURE = 'SNMP_AUTH_FAILURE',
  PORT_DOWN = 'PORT_DOWN',
  PORT_UP = 'PORT_UP'
}

export enum ComponentType {
  CPU = 'CPU',
  INTERFACE = 'INTERFACE',
  STORAGE = 'STORAGE',
  NODE = 'NODE'
}

export enum EventMetrics {
  SNMP_TRAP = 'SNMP_TRAP',
  INTERNAL = 'INTERNAL'
}

export enum ThresholdMetrics {
  OVER_UTILIZATION = 'OVER_UTILIZATION',
  SATURATION = 'SATURATION',
  ERRORS = 'ERRORS'
}

export enum ThresholdLevels {
  ABOVE = 'ABOVE',
  EQUAL_TO = 'EQUAL_TO',
  BELOW = 'BELOW',
  NOT_EQUAL_TO = 'NOT_EQUAL_TO',
}

export enum Unknowns {
  UNKNOWN_EVENT = 'UNKNOWN_EVENT',
  UNKNOWN_UNIT = 'UNKNOWN_UNIT'
}

export const conditionLetters = ['a', 'b', 'c', 'd']
