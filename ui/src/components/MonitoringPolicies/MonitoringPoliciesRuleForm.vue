<template>
  <div
    class="rule-form-container"
    v-if="store.selectedPolicy"
  >
    <div>
      <FeatherButton
        primary
        @click="store.displayRuleForm()"
        data-test="new-rule-btn"
      >
        <FeatherIcon :icon="addIcon" />
        New Rule
      </FeatherButton>
      <MonitoringPoliciesExistingItems
        title="Existing Rules"
        :list="store.selectedPolicy.rules"
        :selectedItemId="store.selectedRule?.id"
        @selectExistingItem="populateForm"
      />
    </div>
    <transition name="fade-instant">
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
              label=""
              hideLabel
              v-focus
              data-test="rule-name-input"
              :readonly="store.selectedPolicy.isDefault"
            />
          </div>
          <div class="col">
            <div class="subtitle">Component Type</div>
            <BasicSelect
              :list="componentTypeOptions"
              @item-selected="selectComponentType"
              :selectedId="store.selectedRule.componentType"
              :disabled="store.selectedPolicy.isDefault"
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
              :disabled="store.selectedPolicy.isDefault"
            />
          </div>
          <div class="col">
            <div class="subtitle">
              Event Metrics
            </div>
            <BasicSelect
              :list="metricOptions"
              @item-selected="selectMetric"
              :selectedId="store.selectedRule.metricName"
              :disabled="store.selectedPolicy.isDefault"
            />
          </div>
        </div>
        <div
          class="row"
          v-if="store.selectedRule!.triggerEvents.length"
        >
          <div class="col">
            <div class="form-title">Set Alert Conditions</div>
            <template v-if="store.selectedRule!.detectionMethod === DetectionMethodTypes.THRESHOLD">
              <MonitoringPoliciesThresholdCondition
                v-for="(cond, index) in store.selectedRule!.triggerEvents"
                :key="cond.id"
                :index="index"
                :condition="(cond as ThresholdCondition)"
                @updateCondition="(condition) => store.updateCondition(cond.id, condition)"
                @deleteCondition="(id: string) => store.deleteCondition(id)"
              />
            </template>
            <template v-else>
              <MonitoringPoliciesEventCondition
                v-for="(cond, index) in store.selectedRule!.triggerEvents"
                :key="cond.id"
                :condition="(cond as EventCondition)"
                :policy="store.selectedPolicy"
                :rule="store.selectedRule"
                :index="index"
                @updateCondition="(condition) => store.updateCondition(cond.id, condition)"
                @deleteCondition="(id: string) => store.deleteCondition(id)"
              />
            </template>
            <FeatherButton
              class="add-params"
              text
              @click="store.addNewCondition"
              :disabled="store.selectedRule.triggerEvents.length === 4 || store.selectedPolicy.isDefault"
            >
              Additional Parameters
            </FeatherButton>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import {useMonitoringPoliciesStore} from '@/store/Views/monitoringPoliciesStore'
import {EventCondition, Rule, ThresholdCondition} from '@/types/policies'
import Add from '@featherds/icon/action/Add'
import {ComponentType, DetectionMethodTypes, EventMetrics, ThresholdMetrics} from './monitoringPolicies.constants'

const store = useMonitoringPoliciesStore()
const addIcon = markRaw(Add)

const metricOptions = computed(() => {
  return store.selectedRule?.detectionMethod === DetectionMethodTypes.THRESHOLD
    ? thresholdMetricsOptions
    : eventMetricsOptions
})

const componentTypeOptions = [
  { id: ComponentType.ANY, name: 'Any' },
  { id: ComponentType.SNMP_INTERFACE, name: 'SNMP Interface' },
  { id: ComponentType.SNMP_INTERFACE_LINK, name: 'SNMP Interface Link'},
  { id: ComponentType.NODE, name: 'Node' }
]

const detectionMethodOptions = [
  // { id: DetectionMethodTypes.THRESHOLD, name: 'Threshold' }, BE not ready yet
  { id: DetectionMethodTypes.EVENT, name: 'Event' }
]

const thresholdMetricsOptions = [
  { id: ThresholdMetrics.OVER_UTILIZATION, name: 'Over Utilization' },
  { id: ThresholdMetrics.SATURATION, name: 'Saturation' },
  { id: ThresholdMetrics.ERRORS, name: 'Errors' }
]

const eventMetricsOptions = [
  { id: EventMetrics.SNMP_TRAP, name: 'SNMP Trap' }
  // { id: EventMetrics.INTERNAL, name: 'Internal' } BE Not ready yet
]

const selectComponentType = (type: string) => (store.selectedRule!.componentType = type)
const selectMetric = (metric: string) => (store.selectedRule!.metricName = metric)
const populateForm = (rule: Rule) => store.displayRuleForm(rule)

const selectDetectionMethod = (method: string) => {
  store.selectedRule!.detectionMethod = method
  store.resetDefaultConditions()
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/mixins/elevation';
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins';
@use '@/styles/_transitionFade';
@use '@/styles/vars.scss';

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
    border-radius: vars.$border-radius-s;
    overflow: hidden;

    .form-title {
      @include typography.headline3;
      margin-bottom: var(variables.$spacing-m);
    }
    .subtitle {
      @include typography.subtitle1;
    }
    .add-params {
      margin-top: var(variables.$spacing-xl);
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

  // for event / threshold child conditions
  :deep(.condition-title) {
    display: flex;
    justify-content: space-between;
    margin: var(variables.$spacing-xl) 0 var(variables.$spacing-xs) 0;
    .subtitle {
      @include typography.subtitle1;
    }
    .delete {
      cursor: pointer;
      color: var(variables.$primary);
    }
  }
}

// needed to fix feather issue where
// hideLabel doesn't hide the label
:deep(.label-border) {
  width: 0 !important;
}
</style>
