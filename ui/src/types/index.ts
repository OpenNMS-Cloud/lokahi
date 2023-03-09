import { SORT } from '@featherds/table'
import { PointerAlignment, PopoverPlacement } from '@featherds/popover'

export declare type fncVoid = () => void
export declare type fncArgVoid = (...args: unknown[]) => void

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

export interface IIcon {
  image: any
  title?: string
  tooltip?: string
  size?: number // rem
  cursorHover?: string
}

export interface IInputButtonPopover {
  placement?: PopoverPlacement
  alignment?: PointerAlignment
  icon: IIcon
  label?: string
  handler: fncVoid
}

export const enum Monitor {
  ICMP = 'ICMP',
  SNMP = 'SNMP',
  ECHO = 'ECHO'
}
