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

      <!-- Awaiting BE changes -->
      <!-- <div class="alerts-box status-box border">
        <div class="subtitle2">Status</div>
        <div class="list">
          <AlertsSeverityCard
            v-for="status in statuses"
            :key="status.label"
            :severity="status.label"
            :class="status.label.toLowerCase()"
            :isFilter="isFilter"
            :timeRange="timeRange"
            :isStatus="true"
            :externalCount="status.count"
          />
        </div>
      </div> -->
    </div>
  </div>
</template>

<script lang="ts" setup>
import { Severity, TimeRange } from '@/types/graphql'

defineProps<{
  isFilter?: boolean
  timeRange?: TimeRange
}>()

const severitiesDisplay = ['critical', 'major', 'minor', 'warning']
const severities = Object.values(Severity).filter((s) => severitiesDisplay.includes(s.toLowerCase()))

// Awaiting BE changes
// const statuses = [
//   { label: 'Acknowledged', count: 32 },
//   { label: 'Unacknowledged', count: 20 }
// ]

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
