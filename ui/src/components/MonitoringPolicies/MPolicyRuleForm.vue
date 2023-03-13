<template>
  <div class="rule-form-container" v-if="store.selectedPolicy">
    <div>
      <FeatherButton primary @click="store.displayRuleForm()">
        <FeatherIcon :icon="addIcon" />
        Create New Rule
      </FeatherButton>
      <MPolicyExistingItems
        title="Existing Rules"
        :list="[{ name: 'Test'}, { name: 'Testlongername'}]" 
      />
    </div>
    <transition name="fade">
      <div class="rule-form" v-if="store.selectedRule">
        <div class="form-title">
          Create New Rule
        </div>
        <div class="form-subtitle">
          New Rule Name
        </div>
        <FeatherInput
          v-model="store.selectedRule.name"
          label="New Rule Name"
        />
        <div>
          <div class="subtitle">Component Type</div>
          <BasicSelect
            :list="componentTypeOptions"
            @item-selected="selectComponentType"
          />
        </div>
        <div>
          <div class="subtitle">Detection Method</div>
          <BasicSelect
            :list="detectionMethodOptions"
            @item-selected="selectDetectionMethod"
          />
        </div>
        <div>
          <div class="subtitle">Threshold Metrics</div>
          <BasicSelect
            :list="thresholdMetricsOptions"
            @item-selected="selectThresholdMetrics"
          />
        </div>
        <AlertConditions />
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import Add from '@featherds/icon/action/Add'

const store = useMonitoringPoliciesStore()
const addIcon = markRaw(Add)

const componentTypeOptions = [
  { id: 'cpu', name: 'CPU' },
  { id: 'interface', name: 'Interface' },
  { id: 'storage', name: 'Storage' },
  { id: 'node', name: 'Node' },
]

const detectionMethodOptions = [
  { id: 'threshold', name: 'Threshold' },
  { id: 'event', name: 'Event' }
]

const thresholdMetricsOptions = [
  { id: 'over-utilization', name: 'Over Utilization' },
  { id: 'saturation', name: 'Saturation' },
  { id: 'errors', name: 'Errors' },
]

const selectComponentType = (type: string) => store.selectedRule!.componentType = type
const selectDetectionMethod = (method: string) => store.selectedRule!.detectionMethod = method
const selectThresholdMetrics = (metric: string) => store.selectedRule!.metricName = metric
</script>

<style scoped lang="scss">
@use "@featherds/styles/mixins/elevation";
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins';
@use "@/styles/_transitionFade";

.rule-form-container {
  display: flex;
  flex-direction: column;
  gap: var(variables.$spacing-l);

  .rule-form {
    @include elevation.elevation(2);
    display: flex;
    flex: 1;
    flex-direction: column;
    background: var(variables.$surface);
    padding: var(variables.$spacing-l);
    border-radius: 5px;

    .form-title {
      @include typography.headline3;
    }
    .form-subtitle {
      @include typography.subtitle1;
      margin: var(variables.$spacing-m) 0;
    }
  }

  @include mediaQueriesMixins.screen-md {
    flex-direction: row;
  }
}
</style>