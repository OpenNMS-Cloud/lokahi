<template>
  <div class="detection-method">
    <div class="title">Detection Method</div>
    <FeatherTabContainer class="tab-container">
      <template v-slot:tabs>
        <FeatherTab @click="store.selectedRule.detectionMethod = 'thresholdAlert'">Threshold Alert</FeatherTab>
        <FeatherTab @click="store.selectedRule.detectionMethod = 'event'">Event</FeatherTab>
      </template>
      <FeatherTabPanel class="panel"><ThresholdAlert /></FeatherTabPanel>
      <FeatherTabPanel class="panel">Event </FeatherTabPanel>
    </FeatherTabContainer>
  </div>
</template>

<script lang="ts" setup>
import { FeatherTab, FeatherTabContainer, FeatherTabPanel } from '@featherds/tabs'

import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'

const store = useMonitoringPoliciesStore()
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.detection-method {
  margin-top: var(variables.$spacing-xl);
  padding: var(variables.$spacing-xl) 0;
  border-top: 1px solid var(variables.$shade-4);
  border-bottom: 1px solid var(variables.$shade-4);

  .title {
    @include typography.headline4;
    margin-bottom: var(variables.$spacing-s);
  }
}

:deep(.feather-input-sub-text) {
  display: none;
}
</style>
