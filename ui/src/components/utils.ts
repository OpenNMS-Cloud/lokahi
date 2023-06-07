import { add, intervalToDuration } from 'date-fns'
import { TimeUnit } from '@/types'

/**
 * Translate value to human-readeable duration
 * @param value in seconds/milliseconds
 * @param unit of value - ms by default
 * @returns A shorted version (e.g. 28d4h22m16s)
 */
export const getHumanReadableDuration = (value: number | undefined, unit = TimeUnit.MSecs) => {
  if (value === undefined) return '--'
  if (value === 0) return '0'

  const secs = unit === TimeUnit.Secs ? value : value / 1000
  if (secs < 1) return value + 'ms'

  const duration = intervalToDuration({
    start: new Date(),
    end: add(new Date(), { seconds: secs })
  })

  const days = duration.days ? duration.days + 'd' : ''
  const hours = duration.hours ? duration.hours + 'h' : ''
  const minutes = duration.minutes ? duration.minutes + 'm' : ''
  const seconds = duration.seconds ? duration.seconds + 's' : ''

  return days + hours + minutes + seconds
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

/**
 * Useful for displaying enums in templates
 * COLD_REBOOT -> Cold Reboot
 */
export const snakeToTitleCase = (snakeCase: string | undefined) => {
  if (!snakeCase) return

  return snakeCase
    .toLowerCase()
    .replace(/^[-_]*(.)/, (_, c) => c.toUpperCase())
    .replace(/[-_]+(.)/g, (_, c) => ' ' + c.toUpperCase())
}

const convertBase64ToArrayBuffer = (base64: string) => {
  const binaryString = window.atob(base64)
  const bytes = new Uint8Array(binaryString.length)
  return bytes.map((byte, i) => binaryString.charCodeAt(i))
}

export const createAndDownloadBlobFile = (base64: string, filename: string) => {
  const data = convertBase64ToArrayBuffer(base64)
  const blob = new Blob([data])

  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', filename)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
