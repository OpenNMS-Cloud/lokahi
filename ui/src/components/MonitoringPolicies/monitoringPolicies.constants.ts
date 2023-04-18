export enum DetectionMethodTypes {
  THRESHOLD = 'THRESHOLD',
  EVENT = 'EVENT'
}

export enum SNMPEventType {
  SNMP_COLD_START = 'SNMP_COLD_START',
  SNMP_WARM_START = 'SNMP_Warm_Start',
  SNMP_AUTHEN_FAILURE = 'SNMP_Authen_Failure',
  SNMP_LINK_DOWN ='SNMP_Link_Down',
  SNMP_LINK_UP = 'SNMP_Link_Up',
  SNMP_EGP_DOWN = 'SNMP_EGP_Down'
}

export enum ComponentType {
  ANY = 'ANY',
  //CPU = 'CPU', FIXME: is this a valid type?
  SNMP_INTERFACE = 'SNMP_INTERFACE',
  SNMP_INTERFACE_LINK = 'SNMP_INTERFACE_LINK',
  //STORAGE = 'STORAGE', FIXME: is this a valid type?
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
