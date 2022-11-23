// TODO: cause error when importing
/* export enum NodeDetailContentType {
  MONITORED,
  DETECTED
} */

interface NodeDetail {
  header: string
}

export interface TabNode {
  type: number,
  label: string,
  nodes: NodeDetail[]
}