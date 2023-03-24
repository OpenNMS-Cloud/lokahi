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
              :list="metricOptions"
              @item-selected="selectMetric"
              :selectedId="store.selectedRule.metricName"
            />
          </div>
          <div
            class="col"
            v-if="store.selectedRule.detectionMethod === DetectionMethodTypes.Event"
          >
            <div class="subtitle">Trigger Event</div>
            <BasicSelect
              :list="eventTriggerOptions"
              @item-selected="selectEventTrigger"
              :selectedId="store.selectedRule.eventTrigger"
            />
          </div>
        </div>
        <div
          class="row"
          v-if="store.selectedRule!.conditions.length"
        >
          <div class="col">
            <div class="form-title">Set Alert Conditions</div>
            <MonitoringPoliciesThresholdCondition
              v-if="store.selectedRule!.detectionMethod === DetectionMethodTypes.Threshold"
              v-for="(cond, index) in store.selectedRule!.conditions"
              :index="index"
              :condition="(cond as ThresholdCondition)"
              @updateCondition="(condition) => store.updateCondition(cond.id, condition)"
              @deleteCondition="(id: string) => store.deleteCondition(id)"
            />
            <MonitoringPoliciesEventCondition
              v-else
              v-for="(cond, index) in store.selectedRule!.conditions"
              :condition="(cond as EventCondition)"
              :index="index"
              @updateCondition="(condition) => store.updateCondition(cond.id, condition)"
              @deleteCondition="(id: string) => store.deleteCondition(id)"
            />
            <FeatherButton
              class="add-params"
              text
              @click="store.addNewCondition"
              :disabled="store.selectedRule.conditions.length === 4"
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
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import { EventCondition, IRule, ThresholdCondition } from '@/types/policies'
import Add from '@featherds/icon/action/Add'
import { DetectionMethodTypes } from './monitoringPolicies.constants'

const store = useMonitoringPoliciesStore()
const addIcon = markRaw(Add)

const metricOptions = computed(() => {
  return store.selectedRule?.detectionMethod === DetectionMethodTypes.Threshold
    ? thresholdMetricsOptions
    : eventMetricsOptions
})

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

const eventMetricsOptions = [
  { id: 'snmp-trap', name: 'SNMP Trap' },
  { id: 'internal', name: 'Internal' }
]

const eventTriggerOptions = [
  { id: 'device-cold-reboot', name: 'Device Cold Reboot' },
  { id: 'snmp-auth-fail', name: 'SNMP Authentication Failure' },
  { id: 'port-down', name: 'Port Down' }
]

const selectComponentType = (type: string) => (store.selectedRule!.componentType = type)
const selectMetric = (metric: string) => (store.selectedRule!.metricName = metric)
const populateForm = (rule: IRule) => store.displayRuleForm(rule)

const selectEventTrigger = (trigger: string) => {
  store.selectedRule!.eventTrigger = trigger
  store.resetDefaultConditions()
}

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
