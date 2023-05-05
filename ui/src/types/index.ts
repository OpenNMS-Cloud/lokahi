import { SORT } from '@featherds/table'
import { PointerAlignment, PopoverPlacement } from '@featherds/popover'
// import { ComputedRef } from 'vue'

export * from './flows.d'
export declare type fncVoid = () => void
export declare type fncArgVoid = (...args: any[]) => void

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

export interface FeatherRadioObject {
  name: string
  value: any
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

export interface ChartData {
  labels?: any[]
  datasets: any[]
}

export const enum Monitor {
  ICMP = 'ICMP',
  SNMP = 'SNMP',
  ECHO = 'ECHO'
}

export interface TagSelectItem {
  name: string
  id?: string
  _text?: string
  tenantId?: string
}

export interface ContextMenuItem {
  label: string
  handler: fncVoid
}

export interface IButtonTextIcon {
  label: string | undefined
  type?: string
}
