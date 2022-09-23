<template>
  <MinionsTable :widgetProps="widgetStore.minionWidgetProps" />
</template>

<script setup lang="ts">
  import { useWidgetStore } from '@/store/Views/widgetStore'
  const widgetStore = useWidgetStore()
</script>
