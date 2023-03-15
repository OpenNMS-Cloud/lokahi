<template>
  <div
    class="rule-form-container"
    v-if="store.selectedPolicy"
  >
    <div>
      <FeatherButton
        primary
        @click="store.displayRuleForm()"
      >
        <FeatherIcon :icon="addIcon" />
        Create New Rule
      </FeatherButton>
      <MonitoringPoliciesExistingItems
        title="Existing Rules"
        :list="store.selectedPolicy.rules"
        :selectedItemId="store.selectedRule?.id"
        @selectExistingItem="populateForm"
      />
    </div>
    <transition name="fade">
      <div
        class="rule-form"
        v-if="store.selectedRule"
      >
        <div class="form-title">Create New Rule</div>

        <div class="row">
          <div class="col">
            <div class="subtitle">New Rule Name</div>
            <FeatherInput
              v-model="store.selectedRule.name"
              label="New Rule Name"
            />
          </div>
          <div class="col">
            <div class="subtitle">Component Type</div>
            <BasicSelect
              :list="componentTypeOptions"
              @item-selected="selectComponentType"
              :selectedId="store.selectedRule.componentType"
            />
          </div>
        </div>

        <div class="row">
          <div class="col">
            <div class="subtitle">Detection Method</div>
            <BasicSelect
            :list="detectionMethodOptions"
              @item-selected="selectDetectionMethod"
              :selectedId="store.selectedRule.detectionMethod"
            />
          </div>
          <div class="col">
            <div class="subtitle">Threshold Metrics</div>
            <BasicSelect
            :list="thresholdMetricsOptions"
            @item-selected="selectThresholdMetrics"
            :selectedId="store.selectedRule.metricName"
            />
          </div>
        </div>
        <AlertConditions />
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import { IRule } from '@/types/policies'
import Add from '@featherds/icon/action/Add'

const store = useMonitoringPoliciesStore()
const addIcon = markRaw(Add)

const componentTypeOptions = [
  { id: 'cpu', name: 'CPU' },
  { id: 'interface', name: 'Interface' },
  { id: 'storage', name: 'Storage' },
  { id: 'node', name: 'Node' }
]

const detectionMethodOptions = [
  { id: 'threshold', name: 'Threshold' },
  { id: 'event', name: 'Event' }
]

const thresholdMetricsOptions = [
  { id: 'over-utilization', name: 'Over Utilization' },
  { id: 'saturation', name: 'Saturation' },
  { id: 'errors', name: 'Errors' }
]

const selectComponentType = (type: string) => (store.selectedRule!.componentType = type)
const selectDetectionMethod = (method: string) => (store.selectedRule!.detectionMethod = method)
const selectThresholdMetrics = (metric: string) => (store.selectedRule!.metricName = metric)
const populateForm = (rule: IRule) => (store.displayRuleForm(rule))
</script>

<style scoped lang="scss">
@use '@featherds/styles/mixins/elevation';
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins';
@use '@/styles/_transitionFade';

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
      margin-bottom: var(variables.$spacing-m)
    }
    .subtitle {
      @include typography.subtitle1;
    }
  }

  @include mediaQueriesMixins.screen-md {
    flex-direction: row;
  }

  .row {
    @include mediaQueriesMixins.screen-lg {
      display: flex;
      gap: var(variables.$spacing-xl);
      .col {
        flex: 1;
      }
    }
  }

}
</style>
