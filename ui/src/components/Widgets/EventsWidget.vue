<template>
  <EventsTable :widgetProps="widgetStore.deviceStatusWidgetProps" :data="props.data" />
</template>

<script setup lang="ts">
import { useWidgetStore } from '@/store/Views/widgetStore'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  }
})

const widgetStore = useWidgetStore()
</script>
