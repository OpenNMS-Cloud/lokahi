<template>
  <FeatherDialog
    :labels="{ title: 'Delete Discovery', close: 'Close' }"
    :modelValue="discoveryStore.deleteModalOpen"
  >
    <div class="discovery-dialog">
      Are you sure you want to delete the discovery named {{ discoveryStore?.selectedDiscovery?.name }}?
    </div>

    <template v-slot:footer>
      <FeatherButton @click="discoveryStore.closeDeleteModal">Cancel</FeatherButton>
      <FeatherButton
        @click="() => discoveryStore.deleteDiscovery()"
        primary
        >Yes</FeatherButton
      >
    </template>
  </FeatherDialog>
</template>
<script setup lang="ts">
import { useDiscoveryStore } from '@/store/Views/discoveryStore'

const discoveryStore = useDiscoveryStore()
</script>
