import { defineStore } from 'pinia'
import { FeatherSortObject } from '@/types'
import { DeviceDto, AlarmDto, LocationDto } from '@/types/graphql'
import { LatLngLiteral } from 'leaflet'
import { SORT } from '@featherds/table'
import { numericSeverityLevel } from '@/components/Map/utils'
import { useMapQueries } from '@/store/Queries/mapQueries'
import { AlarmModificationQueryVariable } from '@/types/map'

export const useMapStore = defineStore('mapStore', () => {
  const mapQueries = useMapQueries()
  
  const areDevicesFetching = computed(() => mapQueries.isFetching)
  const devicesWithCoordinates = computed(() => mapQueries.devices.filter((device: DeviceDto) => device.location?.latitude && device.location.longitude))

  const devicesInbounds = computed(() => mapQueries.devices.filter((device: DeviceDto) => {
    const location: LatLngLiteral = {
      lat: device.location?.latitude || -9999999.99,
      lng: device.location?.longitude || -9999999.99
    }
    return mapBounds.value?.contains(location)
  }))

  const alarms: AlarmDto[] = [],
    interestedDevicesID: string[] = [],
    mapCenter: LocationDto = { latitude: 37.776603506225115, longitude: -33.43824554266541 },
    mapBounds = ref(),
    selectedSeverity = 'NORMAL',
    searchedDeviceLabels: string[] = [],
    deviceSortObject: FeatherSortObject = { property: 'label', value: SORT.ASCENDING },
    alarmSortObject: FeatherSortObject = { property: 'id', value: SORT.DESCENDING }
  
  const fetchAlarms = () => []

  const getDeviceAlarmSeverityMap = (): Record<string, string> => {
    const map: { [x: string]: string } = {}

    alarms.forEach((alarm: AlarmDto) => {
      if(alarm.nodeLabel) {
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
    devicesWithCoordinates,
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
