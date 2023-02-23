import { SORT } from '@featherds/table'

export interface SnackbarProps {
  msg: string
  center?: boolean
  error?: boolean
}

export interface IdLabelProps {
  id: string
  label: string
}

export interface FeatherSortObject {
  property: string
  value: SORT | any
}

export enum TimeUnit {
  Secs,
  MSecs
}

export declare type fncVoid = () => void
export declare type fncArgVoid = (...args: unknown[]) => void

export interface IIcon {
  image: any
  title?: string
  tooltip?: string
  size?: string
  cursorHover?: string
}

export const enum Monitor {
  ICMP = 'ICMP',
  SNMP = 'SNMP',
  ECHO = 'ECHO'
}
