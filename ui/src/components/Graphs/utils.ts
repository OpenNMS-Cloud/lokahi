import { fromUnixTime, format } from 'date-fns'

export const formatTimestamp = (timestamp: number, formatStr: string) => {
  const date = fromUnixTime(timestamp / 1000)

  switch (formatStr) {
    case 'minutes':
      return format(date, 'HH:mm:ss')
    case 'hours':
      return format(date, 'HH:mm')
    case 'days':
      return format(date, 'dd/MMM HH:mm')
    case 'months':
      return format(date, 'dd/MMM')
    case 'years':
      return format(date, 'MMM/y')
    default:
      return format(date, 'dd/MMM :HH:mm')
  }
}
