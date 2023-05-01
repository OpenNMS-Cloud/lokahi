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

onBeforeMount(() => nodeStatusStore.setNodeId(Number(route.params.id)))
</script>
  
<style lang="scss" scoped>
</style>