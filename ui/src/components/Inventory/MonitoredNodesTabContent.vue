<template>
  {{ tab.label }}
  <Card v-for="node in tab.nodes" :key="node.header" :item="node" />
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import { TabNode } from '@/types/inventory'
  
defineProps({
  tab: {
    type: Object as PropType<TabNode>,
    required: true
  }
})
</script>

<style lang="scss" scoped></style>