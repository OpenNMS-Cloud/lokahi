import { FlowsApplicationData, FlowsLineChartItem } from '@/types'

// Look into array of objects, if data.label doesn't exist, create a new object
// [{ label : object.label,
// data: [{timestamp : object.timestamp, value : object.value, direction: object.direction}]}]
// If the label already exists, just push the data object to the existing data array.
export const flowsAppDataToChartJSDirection = (data: FlowsApplicationData[], direction: 'INGRESS' | 'EGRESS') => {
  return data.reduce((result: any, { label, ...rest }) => {
    if (rest.direction === direction) {
      const existingItem = result.find((r: any) => r.label === label)
      if (existingItem) {
        existingItem.data.push(rest)
      } else {
        result.push({ label, data: [rest] })
      }
    }
    return result
  }, []) as FlowsLineChartItem[]
}

export const flowsAppDataToChartJSTotal = (data: FlowsApplicationData[]) => {
  const totalData = getTotalOfApplicationSeries(data)

  return totalData.reduce((result: any, { label, ...rest }) => {
    const existingItem = result.find((r: any) => r.label === label)
    if (existingItem) {
      existingItem.data.push(rest)
    } else {
      result.push({ label, data: [rest] })
    }
    return result
  }, []) as FlowsLineChartItem[]
}

const getTotalOfApplicationSeries = (data: FlowsApplicationData[]) => {
  // create a map to store objects grouped by label and timestamp
  const groupedObjects = new Map()

  // iterate over all objects and group them by label and timestamp
  data.forEach((obj) => {
    const key = obj.label + obj.timestamp
    const group = groupedObjects.get(key) || []
    group.push(obj)
    groupedObjects.set(key, group)
  })

  // iterate over groups and create new objects as needed
  const newObjects = [] as FlowsApplicationData[]
  groupedObjects.forEach((group) => {
    if (group.length === 2) {
      const [obj1, obj2] = group
      if (obj1.label === obj2.label && obj1.timestamp === obj2.timestamp) {
        const newObj = {
          label: obj1.label,
          timestamp: obj1.timestamp,
          direction: 'TOTAL',
          value: obj1.value + obj2.value
        }
        newObjects.push(newObj)
      }
    } else {
      const updatedObj = {
        label: group[0].label,
        timestamp: group[0].timestamp,
        direction: 'TOTAL',
        value: group[0].value
      }
      newObjects.push(updatedObj)
    }
  })
  return newObjects
}
