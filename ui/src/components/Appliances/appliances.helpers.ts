import { ExtendedDeviceDTO } from '@/types/device'
import { ExtendedMinionDTO } from '@/types/minion'
import { add, intervalToDuration, formatDuration } from 'date-fns'

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
export const formatItemBgColor = (list: ExtendedMinionDTO[] | ExtendedDeviceDTO[]) => list.map(item => {
  const { icmp_latency: latency, snmp_uptime: uptime, status } = item
  const bg = {
    ok: 'bg-ok',
    failed: 'bg-failed',
    unknown: 'bg-unknown' // undefined | null
  }

  const setBgColor = (metric: any) => {
    let bgColor = bg.unknown
    
    if(![undefined, null].includes(metric)) {
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

export const getHumanReadableDuration = (uptimeInSeconds: number) => {
  let durationDisplay = '--' // undefined | null

  if(uptimeInSeconds >= 0) {
    if (uptimeInSeconds < 60) durationDisplay = `${uptimeInSeconds} ${uptimeInSeconds <= 0 ? '' : 'seconds'}` // 0-59secs
    else {
      const duration = intervalToDuration({
        start: new Date(),
        end: add(new Date(), {seconds: uptimeInSeconds})
      })
  
      durationDisplay = formatDuration(duration, { format: ['days', 'hours', 'minutes']}) // +1min
    }
  }

  return durationDisplay
}

export const formatLatencyDisplay = (latency: any) => {
  let display = '--' // undefined | null

  if(![undefined, null].includes(latency)) {
    if(latency > 0 || latency < 0) display = `${latency}ms`
    else display = latency
  }

  return display
}