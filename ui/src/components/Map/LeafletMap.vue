<template>
  <div v-if="nodesReady" class="geo-map">
    <SeverityFilter />
    <LMap
      ref="map"
      :center="center"
      :max-zoom="19"
      :min-zoom="2"
      :zoomAnimation="true"
      @ready="onLeafletReady"
      @moveend="onMoveEnd"
      @zoom="invalidateSizeFn"
    >
      <template v-if="leafletReady">
        <LControlLayers />
        <LTileLayer
          v-for="tileProvider in tileProviders"
          :key="tileProvider.name"
          :name="tileProvider.name"
          :visible="tileProvider.visible"
          :url="tileProvider.url"
          :attribution="tileProvider.attribution"
          layer-type="base"
        />
        <MarkerCluster
          ref="markerCluster"
          :onClusterUncluster="onClusterUncluster"
          :options="{ showCoverageOnHover: false, chunkedLoading: true, iconCreateFunction }"
        >
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
              <!-- Category: {{ node?.categories?.length ? node?.categories[0].name : 'N/A' }} -->
            </LPopup>
            <LIcon
              :icon-url="setIcon(node as Partial<DefaultNode>)"
              :icon-size="iconSize"
            />
          </LMarker>
          <LPolyline
            v-for="coordinatePair of computedEdges"
            :key="coordinatePair[0].toString()"
            :lat-lngs="[coordinatePair[0], coordinatePair[1]]"
            color="green"
          />
        </MarkerCluster>
      </template>
    </LMap>
  </div>
</template>

<script
  setup
  lang="ts"
>
import 'leaflet/dist/leaflet.css'
import {
  LMap,
  LTileLayer,
  LMarker,
  LIcon,
  LPopup,
  LControlLayers,
  LPolyline
} from '@vue-leaflet/vue-leaflet'
import MarkerCluster from './MarkerCluster.vue'
import NormalIcon from '@/assets/Normal-icon.png'
import WarninglIcon from '@/assets/Warning-icon.png'
import MinorIcon from '@/assets/Minor-icon.png'
import MajorIcon from '@/assets/Major-icon.png'
import CriticalIcon from '@/assets/Critical-icon.png'
import { numericSeverityLevel } from './utils'
import SeverityFilter from './SeverityFilter.vue'
import { useTopologyStore } from '@/store/Views/topologyStore'
import { useMapStore } from '@/store/Views/mapStore'
import useSpinner from '@/composables/useSpinner'
import { DefaultNode } from '@/types/graphql'
import useTheme from '@/composables/useTheme'
// @ts-ignore
import { Map as LeafletMap, divIcon, MarkerCluster as Cluster } from 'leaflet'

const markerCluster = ref()
const computedEdges = ref<number[][][]>()
const topologyStore = useTopologyStore()
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
const center = computed<number[]>(() => ['latitude', 'longitude'].map(k => (mapStore.mapCenter as any)[k] ))
const bounds = computed(() => {
  const coordinatedMap = getNodeCoordinateMap.value
  return mapStore.nodesWithCoordinates.map((node: DefaultNode) => coordinatedMap.get(node?.id))
})
const nodeLabelAlarmServerityMap = computed(() => mapStore.getDeviceAlarmSeverityMap())

// on light / dark mode change, switch the map layer
onThemeChange(() => {
  // set all layers false
  // for (const tileOptions of tileProviders.value) {
  //   tileOptions.visible = false
  // }

  // if (isDark.value) {
  //   defaultDarkTileLayer.value.visible = true // defauly dark
  // } else {
  //   defaultLightTileLayer.value.visible = true // default light
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
  computeEdges()
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

const setIcon = (node?: Partial<DefaultNode>) => setMarkerColor(node?.nodeLabel)

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

const computeEdges = () => {
  const interestedNodesCoordinateMap = getNodeCoordinateMap.value
  const edges = topologyStore.edges

  const edgeCoordinatesPairs:number[][][] = []

  for (const edge of Object.values(edges)) {
    // attempt to get nodes cluster
    let sourceCoord = nodeClusterCoords.value[edge.source]
    let targetCoord = nodeClusterCoords.value[edge.target]

    // if not in cluser, will be undefined, get regular coords
    if (!sourceCoord) {
      sourceCoord = interestedNodesCoordinateMap.get(edge.source)
    }
    if (!targetCoord) {
      targetCoord = interestedNodesCoordinateMap.get(edge.target)
    }

    if (sourceCoord && targetCoord) {
      edgeCoordinatesPairs.push([sourceCoord, targetCoord])
    }
  }

  computedEdges.value = edgeCoordinatesPairs
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
    leafletObject.value.zoomControl.setPosition('topright')
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
  if(!leafletReady.value) return

  return leafletObject.value.invalidateSize()
}

/*****Tile Layer*****/
const defaultLightTileLayer = ref({
  name: 'OpenStreetMap',
  visible: true,
  attribution: '&copy; <a target="_blank" href="http://osm.org/copyright">OpenStreetMap</a> contributors',
  url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
})

// TODO: Find new dark mode layer as this one has issues on prod
// const defaultDarkTileLayer = ref({
//     name: 'AlidadeSmoothDark',
//     visible: isDark.value && true,
//     url: 'https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/{z}/{x}/{y}{r}.png',
//     attribution:
//       '&copy; <a href="https://stadiamaps.com/">Stadia Maps</a>, &copy; <a href="https://openmaptiles.org/">OpenMapTiles</a> &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors'
// })

const tileProviders = ref([
  defaultLightTileLayer.value
  // defaultDarkTileLayer.value
])

onMounted(() => {
  nodesReady.value = computed(() => {
    mapStore.areDevicesFetching ? startSpinner() : stopSpinner()
    return !mapStore.areDevicesFetching
  })
})

defineExpose({ invalidateSizeFn, setBoundingBox, flyToNode })
</script>

<style scoped>
.search-bar {
  position: absolute;
  margin-left: 10px;
  margin-top: 10px;
}
.geo-map {
  height: 100%;
}
</style>

<style lang="scss">
@use "@featherds/styles/themes/variables";

// TODO: scoped the following
.leaflet-marker-pane {
  div {
    width: 30px !important;
    height: 30px !important;
    margin-left: -15px !important;
    margin-top: -15px !important;
    text-align: center;
    font: 12px "Helvetica Neue", Arial, Helvetica, sans-serif;
    border-radius: 15px;
    border: none;
    span {
      border-radius: 15px;
      line-height: 30px;
      width: 100%;
      display: block;
      &.NORMAL {
        background: var(variables.$success);
      }
      &.WARNING {
        background: #fffb00ea;
      }
      &.MINOR {
        background-color: var(variables.$warning);
      }
      &.MAJOR {
        background: #ff3c00;
      }
      &.CRITICAL {
        background: var(variables.$error);
      }
      opacity: 0.7;
    }
  }
}
</style>

