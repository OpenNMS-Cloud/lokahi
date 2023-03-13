export enum DiscoveryType {
  None = 'None',
  ICMP = 'ICMP',
  Azure = 'AZURE',
  SyslogSNMPTraps = 'SyslogSNMPTraps'
}

export enum ContentEditableType {
  IP,
  CommunityString,
  UDPPort
}

export const UDP_PORT = {
  regexDelim: '[,;]',
  default: 161
}

export const COMMUNITY_STRING = {
  regexDelim: '[,;]',
  default: 'public'
}

export const IP_RANGE = {
  regexDelim: '[,;]+'
}

export const REGEX_PORT = {
  onePort:
    /^([1-9]|[1-9]\d{1,3}|[1-5]\d{4}|6[0-4]\d{3}|65[0-4]\d{2}|655[0-2]\d|6553[0-5])(,([1-9]|[1-9]\d{1,3}|[1-5]\d{4}|6[0-4]\d{3}|65[0-4]\d{2}|655[0-2]\d|6553[0-5]))*$/,
  listPorts:
    /^([1-9]|[1-9]\d{1,3}|[1-5]\d{4}|6[0-4]\d{3}|65[0-4]\d{2}|655[0-2]\d|6553[0-5])([,;]([1-9]|[1-9]\d{1,3}|[1-5]\d{4}|6[0-4]\d{3}|65[0-4]\d{2}|655[0-2]\d|6553[0-5]))*$/
}
