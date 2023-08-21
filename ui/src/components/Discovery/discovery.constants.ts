export enum DiscoveryType {
  None = 'None',
  ICMP = 'ICMP',
  ICMPV3NoAuth = 'ICMPV3NoAuth',
  ICMPV3Auth = 'ICMPV3Auth',
  ICMPV3AuthPrivacy = 'ICMPV3AuthPrivacy',
  Azure = 'AZURE',
  SyslogSNMPTraps = 'SyslogSNMPTraps'
}

export enum InstructionsType {
  Active = 'active',
  Passive = 'passive'
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

/** IP regex: [IPv4, IPv4 ranges, IPv6] */
export const REGEX_EXPRESSIONS = {
  PORT: [
    /^([1-9]|[1-9]\d{1,3}|[1-5]\d{4}|6[0-4]\d{3}|65[0-4]\d{2}|655[0-2]\d|6553[0-5])(,([1-9]|[1-9]\d{1,3}|[1-5]\d{4}|6[0-4]\d{3}|65[0-4]\d{2}|655[0-2]\d|6553[0-5]))*$/
  ],
  IP: [
    /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:-(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))?$/,
    /^(?:\d{1,3}\.){3}\d{1,3}(?:\/(?:[1-2]?[0-9]|3[0-2]))?$|^((?:\d{1,3}\.){3}(?:\d{1,3}-){1,2}\d{1,3})$/,
    /^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})|(([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{0,4})?::(([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{0,4})?|(^[0-9a-fA-F]{1,4}:){1,7}[0-9a-fA-F]{1,4}\/(1[0-1][0-9]|12[0-8]|[0-9]|[1-9][0-9])$/
  ]
}
