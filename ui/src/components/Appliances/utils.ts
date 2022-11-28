import { ExtendedNode } from '@/types/node'
import { ExtendedMinionDTO } from '@/types/minion'

export interface BGColors {
  latencyBgColor: string
  uptimeBgColor: string
  statusBgColor: string
}

/**
 * Change color background on metrics value
 * @param list 
 * @returns list of items with added metrics background color props
 */
export const formatItemBgColor = (list: ExtendedMinionDTO[] | ExtendedNode[]) => list.map(item => {
  const { icmp_latency: latency, snmp_uptime: uptime, status } = item
  const bg = {
    ok: 'ok',
    failed: 'failed',
    unknown: 'unknown' // undefined | null
  }

  const setBgColor = (metric: number) => {
    let bgColor = bg.unknown
    
    if(![undefined, null].includes(metric as any)) {
      bgColor = metric >= 0 ? bg.ok : bg.failed
    }

    return bgColor
  } 

  return {
    ...item,
    latencyBgColor: setBgColor(latency),
    uptimeBgColor: setBgColor(uptime),
    statusBgColor: status === 'UP' ? bg.ok : bg.failed
  }
})
