<template>
  <div class="topo-layers-drawer">
    <div class="close-btn">
      <FeatherButton
        icon="close"
        @click="closeDrawer"
      >
        <FeatherIcon :icon="Close" />
      </FeatherButton>
    </div>
    <FeatherList>
      <FeatherListItem
        v-for="graph in graphsDisplay.graphs"
        :key="graph.label"
        @click="selectTopologyGraph(graph.namespace)"
        :class="{ 'selected' : graph.namespace === selectedNamespace }"
        >{{ graph.label }}</FeatherListItem
      >
    </FeatherList>
  </div>
</template>

<script
  setup
  lang="ts"
>
import { useTopologyStore } from '@/store/Views/topologyStore'
import Close from '@featherds/icon/navigation/Cancel'

const topologyStore = useTopologyStore()
const graphsDisplay = computed(() => topologyStore.topologyGraphsDisplay)
const selectedNamespace = ref()

const selectTopologyGraph = (namespace: string) => {
  topologyStore.getTopologyGraphByContainerAndNamespace({ containerId: topologyStore.topologyGraphsDisplay.id, namespace })
}

const namespace = computed(() => topologyStore.namespace)
watch(namespace, (ns) => {
  selectedNamespace.value = ns
})

const closeDrawer = () => topologyStore.isRightDrawerOpen = false
</script>

<style
  scoped
  lang="scss"
>
.topo-layers-drawer {
  width: 100%;
  height: 100%;
  padding: 10px;

  .close-btn {
    display: flex;
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
