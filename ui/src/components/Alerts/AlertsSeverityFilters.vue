<template>
  <div class="summary border">
    <div class="subtitle-container">
      <div class="subtitle">Alerts Summary</div>
    </div>
    <div class="internal-box">
      <div class="alerts-box border">
        <div class="subtitle2">Alerts</div>
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
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { TimeRange } from '@/types/graphql'

defineProps<{
  isFilter?: boolean
  timeRange?: TimeRange
}>()

const severities = ['critical', 'major', 'minor', 'warning', 'indeterminate']

// for setting CSS properties
const gap = 1.5
const itemGap = `${gap}%`
const listItemWidth = `${100 - (gap * (severities.length - 1)) / severities.length}%` // to set card with equal width
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.summary {
  margin-bottom: var(variables.$spacing-l);
  background: var(variables.$surface);
  padding: 30px;

  .subtitle-container {
    display: flex;
    justify-content: space-between;
    .subtitle {
      @include typography.headline3;
      margin-bottom: 30px;
    }
  }

  .internal-box {
    display: flex;
    gap: 20px;
    .alerts-box {
      display: flex;
      flex: 2.5;
      flex-direction: column;
      padding: 20px;

      .subtitle2 {
        @include typography.subtitle1;
      }
    }
    .status-box {
      flex: 1;
    }
  }
}

.list {
  display: flex;
  flex-direction: row;
  gap: v-bind(itemGap);
  > * {
    width: v-bind(listItemWidth);
  }
}
</style>
