<template>
  <FeatherChipList label="List of metric chips">
    <MetricChip v-for="item in items" :key="item.type" :item="item" />
  </FeatherChipList>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import { FeatherChipList } from '@featherds/chips'
import { Chip } from '@/types/metric'
import MetricChip from '../Common/MetricChip.vue'

defineProps({
  items: {
    type: Object as PropType<Chip[]>,
    required: true
  }
})
</script>

<style lang="scss" scoped>
.chip-list {
  margin: 0;
}
</style>