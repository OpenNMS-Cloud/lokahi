import { add, intervalToDuration, formatDuration } from 'date-fns'
import { TimeUnit } from '@/types'

/**
 * Translate timestamp to human-readeable duration
 * @param timestamp seconds/milliseconds
 * @param timeUnit milliseconds by default
 * @returns A shorted version (e.g. 28d4h22m16s) 
 */
export const getHumanReadableDuration = (timestamp: number | undefined, timeUnit = TimeUnit.MSecs) => {
  let durationDisplay = '--' // undefined | null

  if(timestamp !== undefined) {
    const timestampInSecs = timeUnit === TimeUnit.Secs ? timestamp : timestamp / 1000

    if(Math.abs(timestampInSecs) < 1) {
      durationDisplay = String(timestamp)

      if(timestampInSecs !== 0) durationDisplay += 'ms'
    } else {
      const duration = intervalToDuration({
        start: new Date(),
        end: add(new Date(), { seconds: timestampInSecs })
      })
        
      let durationFormatted = formatDuration(duration, { format: ['days', 'hours', 'minutes', 'seconds']})
      const re = /(?<s>seconds?)|(?<m>minutes?)|(?<h>hours?)|(?<d>days?)/gm
    
      // replace days/hours/minutes/seconds by d/h/m/s
      for(const mat of durationFormatted.matchAll(re)) {
        const { s, m, h, d } = mat.groups as Record<string, string>

        if(s) durationFormatted = durationFormatted.replace(s, 's')
        if(m) durationFormatted = durationFormatted.replace(m, 'm')
        if(h) durationFormatted = durationFormatted.replace(h, 'h')
        if(d) durationFormatted = durationFormatted.replace(d, 'd')
      }
    
      durationDisplay = durationFormatted.replaceAll(' ', '')
    }
  }

  return durationDisplay
}

/**
  - viewBox: attribute is required to control the icon dimension
    - @material-design-icons: does not have viewBox prop - need to set it manually on the FeatherIcon component with width/height
  - css: use font-size to set the icon dimension (recommended), with width and height set to 1em (already set by FeatherIcon component)
  - svg: icon rendering props
    - @material-design-icons: only width/height available
    - @featherds: only viewBox available

 * @param icon svg
 * @returns string e.g. '0 0 24 24'
 */
export const setViewBox = (icon: any) => {
  const iconProps = icon.render().props
  
  return iconProps.viewBox || `0 0 ${iconProps.width} ${iconProps.height}`
}
      