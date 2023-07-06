<template>
  <NodeInfoTable />
  <SNMPInterfacesTable v-if="!nodeStatusStore.isAzure" />
  <IPInterfacesTable/>
  <EventsTable />
</template>
  
<script setup lang="ts">
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'

const nodeStatusStore = useNodeStatusStore()
const route = useRoute()

onBeforeMount(() => {
  const nodeId = Number(route.params.id)
  nodeStatusStore.setNodeId(nodeId)
  nodeStatusStore.fetchExporters(nodeId)
})
</script>
  
<style lang="scss" scoped>
</style>