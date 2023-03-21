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
              :list="metricOptions"
              @item-selected="selectMetric"
              :selectedId="store.selectedRule.metricName"
            />
          </div>
          <div
            class="col"
            v-if="store.selectedRule.detectionMethod === events.Event"
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
            <div class="subtitle">Set Alert Conditions</div>
            <MonitoringPoliciesThresholdCondition
              v-if="store.selectedRule!.detectionMethod === events.Threshold"
              v-for="cond in store.selectedRule!.conditions"
              :condition="(cond as ThresholdCondition)"
              @updateCondition="(condition) => store.updateCondition(cond.id, condition)"
            />
            <MonitoringPoliciesEventCondition
              v-else
              v-for="cond in store.selectedRule!.conditions"
              :condition="(cond as EventCondition)"
              @updateCondition="(condition) => store.updateCondition(cond.id, condition)"
            />
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

enum events {
  Threshold = 'threshold',
  Event = 'event'
}

const store = useMonitoringPoliciesStore()
const addIcon = markRaw(Add)

const metricOptions = computed(() => {
  return store.selectedRule?.detectionMethod === events.Threshold ? thresholdMetricsOptions : eventMetricsOptions
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
const selectDetectionMethod = (method: string) => (store.selectedRule!.detectionMethod = method)
const selectMetric = (metric: string) => (store.selectedRule!.metricName = metric)
const selectEventTrigger = (trigger: string) => (store.selectedRule!.eventTrigger = trigger)
const populateForm = (rule: IRule) => store.displayRuleForm(rule)
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
    overflow: hidden;

    .form-title {
      @include typography.headline3;
      margin-bottom: var(variables.$spacing-m);
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
