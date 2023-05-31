import { defineStore } from 'pinia'
import { FeatherSortObject } from '@/types'
import { Node, MonitoringLocation } from '@/types/graphql'
// import { LatLngLiteral } from 'leaflet'
import { SORT } from '@featherds/table'
import { numericSeverityLevel } from '@/components/Map/utils'
import { useMapQueries } from '@/store/Queries/mapQueries'
import { AlarmModificationQueryVariable } from '@/types/map'

export const useMapStore = defineStore('mapStore', () => {
  const mapQueries = useMapQueries()

  const areDevicesFetching = computed(() => mapQueries.isFetching)
  // TODO: Uncomment when nodes have locations
  // const nodesWithCoordinates = computed(() => mapQueries.nodes.filter((node: Node) => node.location?.latitude && node.location.longitude))

  const devicesInbounds = computed(() =>
    mapQueries.nodes.filter((node: Node) => {
      return false
      // const location: LatLngLiteral = {
      //   lat: node.location?.latitude || -9999999.99,
      //   lng: node.location?.longitude || -9999999.99
      // }
      // return mapBounds.value?.contains(location)
    })
  )

  const alarms: any[] = [],
    interestedDevicesID: string[] = [],
    mapCenter: any = { latitude: 37.776603506225115, longitude: -33.43824554266541 },
    mapBounds = ref(),
    selectedSeverity = 'NORMAL',
    searchedDeviceLabels: string[] = [],
    deviceSortObject: FeatherSortObject = { property: 'label', value: SORT.ASCENDING },
    alarmSortObject: FeatherSortObject = { property: 'id', value: SORT.DESCENDING }

  const fetchAlarms = () => []

  const getDeviceAlarmSeverityMap = (): Record<string, string> => {
    const map: { [x: string]: string } = {}

    alarms.forEach((alarm: any) => {
      if (alarm.nodeLabel) {
        if (numericSeverityLevel(alarm.severity) > numericSeverityLevel(map[alarm.nodeLabel])) {
          map[alarm.nodeLabel] = alarm.severity?.toUpperCase() || ''
        }
      }
    })

    return map
  }

  const modifyAlarm = async (alarmQueryVariable: AlarmModificationQueryVariable) => {
    // todo: add graphQL query
    const resp = {}
    return resp
  }

  return {
    areDevicesFetching,
    nodesWithCoordinates: [] as any[], // TODO: fix when nodes have locations
    devicesInbounds,
    alarms,
    interestedDevicesID,
    mapCenter,
    mapBounds,
    selectedSeverity,
    searchedDeviceLabels,
    deviceSortObject,
    alarmSortObject,
    fetchAlarms,
    getDeviceAlarmSeverityMap,
    modifyAlarm
  }
})
