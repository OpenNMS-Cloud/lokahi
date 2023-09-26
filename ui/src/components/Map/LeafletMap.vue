<template>
  <div v-if="nodesReady">
    <LMap
      ref="map"
      :center="center"
      :max-zoom="19"
      :min-zoom="2"
      :zoom="3"
      :zoomAnimation="true"
      @ready="onLeafletReady"
      @moveend="onMoveEnd"
      @zoom="invalidateSizeFn"
      :useGlobalLeaflet="true"
    >
      <template v-if="leafletReady">
        <LTileLayer
          v-for="tileProvider in tileProviders"
          :key="tileProvider.name"
          :name="tileProvider.name"
          :visible="tileProvider.visible"
          :url="tileProvider.url"
          :attribution="tileProvider.attribution"
          layer-type="base"
        />
        <l-marker-cluster-group
          :chunkedLoading="true"
          :options="{ iconCreateFunction }"
          :showCoverageOnHover="false"
          :spiderfyOnEveryZoom="false"
          :spiderfyOnMaxZoom="false"
          :zoomToBoundsOnClick="false"
        >
          <LMarker
            v-for="node of nodes"
            :key="node?.nodeLabel"
            :lat-lng="[node?.location?.latitude, node?.location?.longitude]"
            :name="node?.nodeLabel"
            :options="{ id: node?.id }"
          >
            <LIcon :icon-size="iconSize">
              <MapPin />
            </LIcon>
          </LMarker>
        </l-marker-cluster-group>
      </template>
    </LMap>
  </div>
</template>

<script setup lang="ts">
import L from 'leaflet'
import { LMap, LTileLayer, LMarker, LIcon } from '@vue-leaflet/vue-leaflet'
import { LMarkerClusterGroup } from 'vue-leaflet-markercluster'
import { numericSeverityLevel } from './utils'
import { useMapStore } from '@/store/Views/mapStore'
import useSpinner from '@/composables/useSpinner'
import { Node } from '@/types/graphql'
import useTheme from '@/composables/useTheme'
// @ts-ignore
import { Map as LeafletMap, divIcon, MarkerCluster as Cluster } from 'leaflet'
import 'leaflet/dist/leaflet.css'
import 'vue-leaflet-markercluster/dist/style.css'
import { render, createVNode } from 'vue'
import MapPin from './MapPin.vue'

globalThis.L = L
const { onThemeChange, isDark } = useTheme()
const map = ref()
const route = useRoute()
const leafletReady = ref<boolean>(false)
const leafletObject = ref({} as LeafletMap)
const zoom = ref<number>(2)
const iconWidth = 30
const iconHeight = 80
const iconSize = [iconWidth, iconHeight]
const nodeClusterCoords = ref<Record<string, number[]>>({})

const { startSpinner, stopSpinner } = useSpinner()
const mapStore = useMapStore()
const nodesReady = ref()
const nodes = computed(() => mapStore.nodesWithCoordinates)
const center = computed<number[]>(() => ['latitude', 'longitude'].map((k) => (mapStore.mapCenter)[k]))
const bounds = computed(() => {
  const coordinatedMap = getNodeCoordinateMap.value
  return mapStore.nodesWithCoordinates.map((node: Node) => coordinatedMap.get(node?.id))
})
const nodeLabelAlarmServerityMap = computed(() => mapStore.getDeviceAlarmSeverityMap())

const lightDarkFilter = computed(() => isDark.value ? 'invert(1) hue-rotate(180deg) grayscale(0.3)' : '')

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
  const childCount = cluster.getChildCount()
  const el = document.createElement('div')
  const vNode = createVNode(MapPin, { numberOfNodes: childCount })

  let iconAnchorX = 24
  const iconAnchorY = 41

  if (childCount > 9) {
    iconAnchorX = 27
  }
  if (childCount > 99) {
    iconAnchorX = 30
  }

  render(vNode, el)
  return divIcon({ html: el, iconAnchor: [iconAnchorX, iconAnchorY] })
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
.leaflet-tile-pane,
.leaflet-control-attribution {
  filter: v-bind(lightDarkFilter)
}

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
