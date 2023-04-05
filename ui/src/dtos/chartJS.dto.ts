import { FlowsApplicationChartData, FlowsApplicationData } from '@/types'

// Look into array of objects, if data.label doesn't exist, create a new object
// [{ label : object.label,
// data: [{timestamp : object.timestamp, value : object.value, direction: object.direction}]}]
// If the label already exists, just push the data object to the existing data array.
export const flowsAppDataToChartJS = (data: FlowsApplicationData[]) => {
  return data.reduce((result: any, { label, ...rest }) => {
    const existingItem = result.find((r: any) => r.label === label)
    if (existingItem) {
      existingItem.data.push(rest)
    } else {
      result.push({ label, data: [rest] })
    }
    return result
  }, []) as FlowsApplicationChartData[]
}
