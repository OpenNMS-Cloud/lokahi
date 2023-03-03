export enum DiscoveryType {
  None,
  ICMP,
  Azure,
  SyslogSNMPTraps
}

export enum ContentEditableType {
  IP,
  CommunityString,
  UDPPort
}

export const UDP_PORT = {
  regexDelim: '[,; ]',
  default: 161
}

export const COMMUNITY_STRING = {
  regexDelim: '[,; ]',
  default: 'public'
}

export const IP_RANGE = {
  regexDelim: '[,; ]+'
}
