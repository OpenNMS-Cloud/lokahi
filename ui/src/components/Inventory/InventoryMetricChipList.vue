<template>
  <FeatherChipList label="List of metric chips">
    <MetricChip v-for="metric in metrics" :key="metric?.type" :metric="metric" />
  </FeatherChipList>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import { FeatherChipList } from '@featherds/chips'
import { Chip } from '@/types/metric'
import MetricChip from '../Common/MetricChip.vue'

defineProps({
  metrics: {
    type: Object as PropType<Chip[]>,
    required: true
  }
})
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";

.chip-list {
  margin: 0 0 var(variables.$spacing-s);
  gap: 1rem;
}
</style>
