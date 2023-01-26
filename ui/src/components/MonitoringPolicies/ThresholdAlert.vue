<template>
  <div class="list">
    <div
      class="card"
      :class="{ selected: selectedAlert === 'interfaceUtil' }"
      @click="setThresholdAlert('interfaceUtil')"
    >
      <div class="metric-name">METRIC NAME</div>
      Interface Util
    </div>
    <div
      class="card"
      :class="{ selected: selectedAlert === 'discards' }"
      @click="setThresholdAlert('discards')"
    >
      <div class="metric-name">METRIC NAME</div>

      Discards
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
const store = useMonitoringPoliciesStore()
const selectedAlert = ref('interfaceUtil')

const setThresholdAlert = (type: string) => {
  selectedAlert.value = type
  store.setMetricName(type)
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.list {
  display: flex;
  margin-top: var(variables.$spacing-l);
  > .card {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    padding: var(variables.$spacing-s);
    border: 1px solid var(variables.$shade-4);
    background-color: var(variables.$shade-4);
    margin-right: var(variables.$spacing-m);
    width: 240px;
    border-radius: 5px;
    font-size: 15px;
    @include typography.body-small;

    cursor: pointer;
    &.selected {
      border: 2px solid var(variables.$primary);
      font-weight: 700;
    }

    > .metric-name {
      font-size: 9px;
      font-weight: 400;
      line-height: 16px;
    }
  }
}
:deep(.feather-input-sub-text) {
  display: none;
}
</style>
