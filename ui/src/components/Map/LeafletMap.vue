<template>
  <div v-if="nodesReady">
    <LMap
      ref="map"
      :center="center"
      :max-zoom="19"
      :min-zoom="2"
      :zoom="2"
      :zoomAnimation="true"
      @ready="onLeafletReady"
      @moveend="onMoveEnd"
      @zoom="invalidateSizeFn"
    >
      <template v-if="leafletReady">
        <!-- <LControlLayers /> -->
        <LTileLayer
          v-for="tileProvider in tileProviders"
          :key="tileProvider.name"
          :name="tileProvider.name"
          :visible="tileProvider.visible"
          :url="tileProvider.url"
          :attribution="tileProvider.attribution"
          layer-type="base"
        />
        <!-- <MarkerCluster
          ref="markerCluster"
          :onClusterUncluster="onClusterUncluster"
          :options="{ showCoverageOnHover: false, chunkedLoading: true, iconCreateFunction }"
        > -->
        <LMarker
          v-for="node of nodes"
          :key="node?.nodeLabel"
          :lat-lng="[node?.location?.latitude, node?.location?.longitude]"
          :name="node?.nodeLabel"
          :options="{ id: node?.id }"
        >
          <LPopup>
            Node:
            <router-link
              :to="`/node/${node?.id}`"
              target="_blank"
              >{{ node?.nodeLabel }}</router-link
            >
            <br />
            Severity: {{ nodeLabelAlarmServerityMap[node?.nodeLabel as string] || 'NORMAL' }}
            <br />
          </LPopup>
          <LIcon>
            <MapPin />
          </LIcon>
        </LMarker>
        <!-- </MarkerCluster> -->
      </template>
    </LMap>
  </div>
</template>

<script setup lang="ts">
import 'leaflet/dist/leaflet.css'
import { LMap, LTileLayer, LMarker, LPopup, LIcon } from '@vue-leaflet/vue-leaflet'
import MarkerCluster from './MarkerCluster.vue'
import NormalIcon from '@/assets/Normal-icon.png'
import WarninglIcon from '@/assets/Warning-icon.png'
import MinorIcon from '@/assets/Minor-icon.png'
import MajorIcon from '@/assets/Major-icon.png'
import CriticalIcon from '@/assets/Critical-icon.png'
import { numericSeverityLevel } from './utils'
import { useMapStore } from '@/store/Views/mapStore'
import useSpinner from '@/composables/useSpinner'
import { Node } from '@/types/graphql'
import useTheme from '@/composables/useTheme'
// @ts-ignore
import { Map as LeafletMap, divIcon, MarkerCluster as Cluster } from 'leaflet'

const markerCluster = ref()
const { onThemeChange, isDark } = useTheme()
const map = ref()
const route = useRoute()
const leafletReady = ref<boolean>(false)
const leafletObject = ref({} as LeafletMap)
const zoom = ref<number>(2)
const iconWidth = 25
const iconHeight = 42
const iconSize = [iconWidth, iconHeight]
const nodeClusterCoords = ref<Record<string, number[]>>({})

const { startSpinner, stopSpinner } = useSpinner()
const mapStore = useMapStore()
const nodesReady = ref()
const nodes = computed(() => mapStore.nodesWithCoordinates)
const center = computed<number[]>(() => ['latitude', 'longitude'].map((k) => (mapStore.mapCenter as any)[k]))
const bounds = computed(() => {
  const coordinatedMap = getNodeCoordinateMap.value
  return mapStore.nodesWithCoordinates.map((node: Node) => coordinatedMap.get(node?.id))
})
const nodeLabelAlarmServerityMap = computed(() => mapStore.getDeviceAlarmSeverityMap())

// on light / dark mode change, switch the map layer
onThemeChange(() => {
  // if (isDark.value) {
  //   apply css hack
  // } else {
  //   normal
  // }
})

const getHighestSeverity = (severitites: string[]) => {
  let highestSeverity = 'NORMAL'
  for (const severity of severitites) {
    if (numericSeverityLevel(severity) > numericSeverityLevel(highestSeverity)) {
      highestSeverity = severity
    }
  }
  return highestSeverity
}

const onClusterUncluster = (t: any) => {
  nodeClusterCoords.value = {}
  t.target.refreshClusters()
}

// for custom marker cluster icon
const iconCreateFunction = (cluster: Cluster) => {
  const clusterLatLng = cluster.getLatLng()
  const clusterLatLngArr = [clusterLatLng.lat, clusterLatLng.lng]
  const childMarkers = cluster.getAllChildMarkers()

  // find highest level of severity
  const severitites = []
  for (const marker of childMarkers) {
    // set cluster latlng to each node id
    if (clusterLatLngArr.length) {
      const nodeId = (marker as any).options.id
      nodeClusterCoords.value[nodeId] = clusterLatLngArr
    }

    const markerSeverity = nodeLabelAlarmServerityMap.value[(marker as any).options.name]
    if (markerSeverity) {
      severitites.push(markerSeverity)
    }
  }
  const highestSeverity = getHighestSeverity(severitites)
  return divIcon({ html: `<span class=${highestSeverity}>` + cluster.getChildCount() + '</span>' })
}

const setIcon = (node?: Partial<Node>) => setMarkerColor(node?.nodeLabel)

const setMarkerColor = (severity: string | undefined | null) => {
  if (severity) {
    switch (severity.toUpperCase()) {
      case 'NORMAL':
        return NormalIcon
      case 'WARNING':
        return WarninglIcon
      case 'MINOR':
        return MinorIcon
      case 'MAJOR':
        return MajorIcon
      case 'CRITICAL':
        return CriticalIcon
      default:
        return NormalIcon
    }
  }
  return NormalIcon
}

const getNodeCoordinateMap = computed(() => {
  const map = new Map()

  mapStore.nodesWithCoordinates.forEach((node: any) => {
    map.set(node.id, [node.location.latitude, node.location.longitude])
    map.set(node.nodeLabel, [node.location.latitude, node.location.longitude])
  })

  return map
})

const onLeafletReady = async () => {
  await nextTick()
  leafletObject.value = map.value.leafletObject
  if (leafletObject.value != undefined && leafletObject.value != null) {
    // set default map view port
    leafletObject.value.zoomControl.setPosition('bottomright')
    leafletReady.value = true

    await nextTick()

    // save the bounds to state
    mapStore.mapBounds = leafletObject.value.getBounds()

    try {
      leafletObject.value.fitBounds(bounds.value)
    } catch (err) {
      console.log(err, `Invalid bounds array: ${bounds.value}`)
    }

    // if nodeid query param, fly to it
    if (route.query.nodeid) {
      flyToNode(route.query.nodeid as string)
    }
  }
}

const onMoveEnd = () => {
  zoom.value = leafletObject.value.getZoom()
  mapStore.mapBounds = leafletObject.value.getBounds()
}

const flyToNode = (nodeLabelOrId: string) => {
  const coordinateMap = getNodeCoordinateMap.value
  const nodeCoordinates = coordinateMap.get(nodeLabelOrId)

  if (nodeCoordinates) {
    leafletObject.value.flyTo(nodeCoordinates, 7)
  }
}

const setBoundingBox = (nodeLabels: string[]) => {
  const coordinateMap = getNodeCoordinateMap.value
  const bounds = nodeLabels.map((nodeLabel) => coordinateMap.get(nodeLabel))
  if (bounds.length) {
    leafletObject.value.fitBounds(bounds)
  }
}

const invalidateSizeFn = () => {
  if (!leafletReady.value) return

  return leafletObject.value.invalidateSize()
}

/*****Tile Layer*****/
const defaultLayer = ref({
  name: 'OpenStreetMap',
  visible: true,
  attribution: '&copy; <a target="_blank" href="http://osm.org/copyright">OpenStreetMap</a> contributors',
  url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
})

const tileProviders = ref([
  defaultLayer.value
  // anotherLayer.value
])

onMounted(() => {
  nodesReady.value = computed(() => {
    mapStore.areDevicesFetching ? startSpinner() : stopSpinner()
    return !mapStore.areDevicesFetching
  })
})

defineExpose({ invalidateSizeFn, setBoundingBox, flyToNode })
</script>

<style scoped lang="scss">
@use '@featherds/styles/mixins/elevation';

:deep(.leaflet-control-zoom) {
  @include elevation.elevation(3);
  border-radius: 8px;
}
</style>

<style lang="scss">
//DARK MODE HACK
// .leaflet-tile-pane,
// .leaflet-control-attribution {
//   filter: invert(1) hue-rotate(180deg) grayscale(0.3)
// }

// custom zoom control styles
.leaflet-touch,
.leaflet-bar,
.leaflet-control-layers {
  border: none !important;
  margin-right: 27px !important;
  margin-bottom: 35px !important;
}

.leaflet-control-zoom-in,
.leaflet-control-zoom-out {
  border: none !important;
}

.leaflet-control-zoom-in {
  border-top-left-radius: 8px !important;
  border-top-right-radius: 8px !important;
}

.leaflet-control-zoom-out {
  border-bottom-left-radius: 8px !important;
  border-bottom-right-radius: 8px !important;
}

.leaflet-control-attribution {
  display: none;
}

.leaflet-div-icon {
  border: none;
  background: transparent;
}
</style>
