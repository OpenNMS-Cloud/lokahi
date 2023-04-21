<template>
  <div
    class="list"
    data-test="severity-list"
  >
    <AlertsSeverityCard
      v-for="severity in severities"
      :key="severity"
      :severity="severity"
      :class="severity.toLowerCase()"
      :isFilter="isFilter"
      :timeRange="timeRange"
    />
  </div>
</template>

<script lang="ts" setup>
import { Severity } from '@/types/graphql'
import { TimeRange } from '@/types/graphql'

defineProps<{
  isFilter?: boolean
  timeRange?: TimeRange
}>()

const severitiesDisplay = ['critical', 'major', 'minor', 'warning', 'indeterminate']
const severities = Object.values(Severity).filter((s) => severitiesDisplay.includes(s.toLowerCase()))

// for setting CSS properties
const gap = 1.5
const itemGap = `${gap}%`
const listItemWidth = `${100 - (gap * (severities.length - 1)) / severities.length}%` // to set card with equal width
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';

.list {
  display: flex;
  flex-direction: row;
  gap: v-bind(itemGap);
  margin-bottom: var(variables.$spacing-l);
  > * {
    width: v-bind(listItemWidth);
  }
  .critical {
    order: 0;
  }
  .major {
    order: 1;
  }
  .minor {
    order: 2;
  }
  .warning {
    order: 3;
  }
  .indeterminate {
    order: 4;
  }
}
</style>
