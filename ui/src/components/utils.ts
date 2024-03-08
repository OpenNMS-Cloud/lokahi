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

/**
 * From Stackoverflow: https://stackoverflow.com/questions/10420352/converting-file-size-in-bytes-to-human-readable-string
 * @param bytes Bytes as a number
 * @param si Unit Display Type, true -> metric SI 1000, false -> binary (IEC) 1024
 * @param dp Number of decimal places to show
 * @returns a nicely formatted string
 */
export const humanFileSize = (bytes: number, si = true, dp = 1) => {
  const thresh = si ? 1000 : 1024

  if (Math.abs(bytes) < thresh) {
    return bytes + ' B'
  }

  const units = si
    ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
    : ['KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB']
  let u = -1
  const r = 10 ** dp

  do {
    bytes /= thresh
    ++u
  } while (Math.round(Math.abs(bytes) * r) / r >= thresh && u < units.length - 1)

  return bytes.toFixed(dp) + ' ' + units[u]
}

export const addOpacity = (hex: string, opacity: number) => {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return result
    ? 'rgba(' +
        parseInt(result[1], 16) +
        ', ' +
        parseInt(result[2], 16) +
        ', ' +
        parseInt(result[3], 16) +
        ', ' +
        opacity +
        ')'
    : hex
}

export const getChartGridColor = (isDark: boolean) => {
  return isDark ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)'
}

export const getColorFromFeatherVar = (indexOrString?: number | string, opacity = false) => {
  let color

  const defaultColors = [
    '--feather-categorical1',
    '--feather-categorical2',
    '--feather-categorical3',
    '--feather-categorical4',
    '--feather-categorical5',
    '--feather-categorical6',
    '--feather-categorical7',
    '--feather-categorical8',
    '--feather-categorical9',
    '--feather-categorical0'
  ]

  // return list of colors
  if (!indexOrString) {
    const colors = []
    for (const c of defaultColors) {
      const hex = getComputedStyle(document.documentElement).getPropertyValue(c)
      if (opacity) {
        colors.push(addOpacity(hex, 0.5))
      } else {
        colors.push(hex)
      }
    }
    return colors
  }

  // return specified feather var or index
  if (typeof indexOrString === 'string') {
    color = getComputedStyle(document.documentElement).getPropertyValue(`--feather-${indexOrString}`)
  } else {
    color = getComputedStyle(document.documentElement).getPropertyValue(defaultColors[indexOrString])
  }

  if (opacity) {
    return addOpacity(color, 0.3)
  } else {
    return color
  }
}

/**
 * @param bits Bits as a number
 * @param dp Number of decimal places to show
 * @returns a nicely formatted string
 */
export const humanFileSizeFromBits = (bits: number, dp = 1) => {
  const thresh = 1000

  if (Math.abs(bits) < thresh) {
    return bits + ' b'
  }

  const units = ['kb', 'Mb', 'Gb', 'Tb', 'Pb']

  let u = -1
  const r = 10 ** dp

  do {
    bits /= thresh
    ++u
  } while (Math.round(Math.abs(bits) * r) / r >= thresh && u < units.length - 1)

  return bits.toFixed(dp) + ' ' + units[u]
}
