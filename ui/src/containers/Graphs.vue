<template>
<div class="header">
  Graphs
  <FeatherButton text @click="onDownload">
    Download All
  </FeatherButton>
</div>

<div id="graphs-layout">
  <div class="left-column" v-if="store.fetchIsDone">
    <LineGraph :graph="nodeLatency" />
  </div>
  <div class="right-column">
    <LineGraph :graph="bits" />
  </div>
</div>
</template>
  
<script setup lang="ts">
import { TimeRangeUnit } from '@/types/graphql'
import { GraphProps } from '@/types/graphs'
import { downloadMultipleCanvases } from '@/components/Graphs/utils'
import { useRoute } from 'vue-router'
import { useGraphsQueries } from '@/store/Queries/graphsQueries'

const route = useRoute()
const store = useGraphsQueries()
const instance = computed(() => store.node.ipInterfaces?.[0].ipAddress as string)

const nodeLatency = computed<GraphProps>(() => {
  return {
    label: 'ICMP Response Time',
    metrics: ['response_time_msec'],
    monitor: 'ICMP',
    nodeId: route.params.id as string,
    instance: instance.value,
    timeRange: 10,
    timeRangeUnit: TimeRangeUnit.Minute
  }
})

const bits = computed<GraphProps>(() => {
  return {
    label: 'Bytes In / Out',
    metrics: ['ifInOctets', 'ifOutOctets '],
    monitor: 'ICMP',
    nodeId: route.params.id as string,
    instance: instance.value,
    timeRange: 10,
    timeRangeUnit: TimeRangeUnit.Minute
  }
})


const onDownload = () => {
  const page = document.getElementById('graphs-layout') as HTMLElement
  const canvases = document.getElementsByClassName('canvas') as HTMLCollectionOf<HTMLCanvasElement>
  downloadMultipleCanvases(page, canvases)
}

onMounted(async () => {
  store.setNodeId(Number(route.params.id))
  await store.fetchNode()
})
</script>
  
<style scoped lang="scss">
@use "@featherds/styles/themes/variables";
@use "@featherds/styles/mixins/typography";

.header {
  @include typography.headline3();
  display: flex;
  padding: 6px;
  background: var(variables.$shade-4);
  justify-content: space-between;
}

#graphs-layout {
  display: flex;
  gap: 20px;
  
  .left-column, 
  .right-column {
    display: flex;
    flex-direction: column;
    width: calc(50% - 10px);
  }
}
</style>
  