<template>
  <DevicesTable :widgetProps="widgetStore.deviceWidgetProps" />
</template>

<script setup lang="ts">
import { useWidgetStore } from '@/store/Views/widgetStore'
const widgetStore = useWidgetStore()
</script>
