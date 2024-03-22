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
        <FeatherIcon :icon="icons.addIcon" />
        New Rule
      </FeatherButton>
      <MonitoringPoliciesExistingItemsLegacy
        title="Existing Rules"
        :list="store.selectedPolicy.rules as PolicyRule[]"
        :selectedItemId="store.selectedRule?.id"
        @selectExistingItem="populateForm"
      />
    </div>
    <transition name="fade-instant">
      <div
        class="rule-form"
        v-if="store.selectedRule"
      >
        <div class="rule-form-title-container">
          <div class="form-title">Create New Rule</div>
          <FeatherButton
            v-if="!store.selectedPolicy.isDefault"
            icon="Delete Rule"
            @click="countAlertsAndOpenDeleteModal"
          >
            <FeatherIcon :icon="icons.deleteIcon" />
          </FeatherButton>
        </div>

        <div class="row">
          <div class="col">
            <div class="subtitle">New Rule Name</div>
            <FeatherInput
              v-model.trim="store.selectedRule.name"
              label=""
              hideLabel
              v-focus
              data-test="rule-name-input"
              :error="store.validationErrors.ruleName"
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
            <template v-if="store.selectedRule?.detectionMethod === DetectionMethod.Threshold">
              <div class="subtitle">Metric</div>
              <BasicSelect
                :list="thresholdMetricsOptions"
                @item-selected="selectThresholdMetric"
                :selectedId="store.selectedRule.thresholdMetricName"
                :disabled="store.selectedPolicy.isDefault"
              />
            </template>
            <template v-else-if="store.selectedRule?.detectionMethod === DetectionMethod.Event">
              <div class="subtitle">Event Type</div>
              <BasicSelect
                :list="eventTypeOptions"
                @item-selected="selectEventType"
                :selectedId="store.selectedRule.eventType"
                :disabled="store.selectedPolicy.isDefault"
              />
            </template>
          </div>
        </div>
        <div
          class="row"
          v-if="store.selectedRule!.alertConditions?.length"
        >
          <div class="col">
            <div class="form-title">Set Alert Conditions</div>
            <template v-if="store.selectedRule!.detectionMethod === DetectionMethod.Threshold">
              <MonitoringPoliciesThresholdConditionLegacy
                v-for="(cond, index) in store.selectedRule!.alertConditions"
                :key="cond.id"
                :index="Number(index)"
                :condition="(cond as ThresholdCondition)"
                @updateCondition="(condition: ThresholdCondition) => store.updateCondition(cond.id, condition)"
                @deleteCondition="(id: string) => store.deleteCondition(id)"
              />
            </template>
            <template
              v-else-if="store.selectedRule!.detectionMethod === DetectionMethod.Event
                                 && store.selectedRule?.eventType"
            >
              <MonitoringPoliciesEventConditionLegacy
                v-for="(cond, index) in store.selectedRule!.alertConditions"
                :key="cond.id"
                :condition="(cond as AlertCondition)"
                :event-type="store.selectedRule?.eventType"
                :index="Number(index)"
                :isDisabled="store.selectedPolicy?.isDefault === true"
                @updateCondition="(condition: ThresholdCondition) => store.updateCondition(cond.id, condition)"
                @deleteCondition="(id: string) => store.deleteCondition(id)"
              />
            </template>
            <FeatherButton
              class="add-params"
              text
              @click="store.addNewCondition"
              :disabled="store.selectedRule.alertConditions?.length === 4 || store.selectedPolicy.isDefault"
            >
              Additional Conditions
            </FeatherButton>
          </div>
        </div>
        <FeatherButton
          class="save-btn"
          primary
          @click="store.saveRule"
          :disabled="disableSaveRuleBtn"
          data-test="save-rule-btn"
        >
          Save Rule
        </FeatherButton>
      </div>
    </transition>
  </div>
  <DeleteConfirmationModal
    :isVisible="isVisible"
    :customMsg="deleteMsg"
    :closeModal="() => closeModal()"
    :deleteHandler="() => store.removeRule()"
    :isDeleting="mutations.deleteRuleIsFetching"
  />
</template>

<script setup lang="ts">
import Add from '@featherds/icon/action/Add'
import Delete from '@featherds/icon/action/Delete'
import useModal from '@/composables/useModal'
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import { useMonitoringPoliciesMutations } from '@/store/Mutations/monitoringPoliciesMutations'
import { AlertCondition, DetectionMethod, EventType, ManagedObjectType, PolicyRule } from '@/types/graphql'
import { ThresholdCondition } from '@/types/policies'
import { ThresholdMetrics } from './monitoringPoliciesLegacy.constants'

const { openModal, closeModal, isVisible } = useModal()
const store = useMonitoringPoliciesStore()
const mutations = useMonitoringPoliciesMutations()
const icons = markRaw({
  addIcon: Add,
  deleteIcon: Delete
})

const componentTypeOptions = [
  { id: ManagedObjectType.Any, name: 'Any' },
  { id: ManagedObjectType.SnmpInterface, name: 'SNMP Interface' },
  { id: ManagedObjectType.SnmpInterfaceLink, name: 'SNMP Interface Link' },
  { id: ManagedObjectType.Node, name: 'Node' }
]

const detectionMethodOptions = [
  // { id: DetectionMethod.Threshold, name: 'Threshold' }, BE not ready yet
  // TODO: https://opennms.atlassian.net/browse/HS-750
  { id: DetectionMethod.Event, name: 'Event' }
]

const thresholdMetricsOptions = [
  { id: ThresholdMetrics.OVER_UTILIZATION, name: 'Over Utilization' },
  { id: ThresholdMetrics.SATURATION, name: 'Saturation' },
  { id: ThresholdMetrics.ERRORS, name: 'Errors' }
]

const eventTypeOptions = [
  { id: EventType.SnmpTrap, name: 'SNMP Trap' },
  { id: EventType.Internal, name: 'Internal' }
]

// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
const selectComponentType = (type: ManagedObjectType) => (store.selectedRule!.componentType = type)
// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
const selectThresholdMetric = (metric: string) => (store.selectedRule!.thresholdMetricName = metric)
// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
const selectEventType = (eventType: EventType) => (store.selectedRule!.eventType = eventType)
const populateForm = async (rule: PolicyRule) => await store.displayRuleForm(rule)

const selectDetectionMethod = async (method: DetectionMethod) => {
  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  store.selectedRule!.detectionMethod = method
  await store.resetDefaultConditions()
}

const disableSaveRuleBtn = computed(
  () => store.selectedPolicy?.isDefault || !store.selectedRule?.name || !store.selectedRule?.alertConditions?.length
)

const countAlertsAndOpenDeleteModal = async () => {
  await store.countAlertsForRule()
  openModal()
}

const deleteMsg = computed(() =>
  `Deleting rule ${store.selectedRule?.name} removes ${store.numOfAlertsForRule} associated alerts. Do you wish to proceed?`
)
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins';
@use '@/styles/_transitionFade';
@use '@/styles/vars.scss';

.rule-form-container {
  display: flex;
  flex-direction: column;
  gap: var(variables.$spacing-l);
  margin-bottom: var(variables.$spacing-xl);

  .rule-form {
    display: flex;
    flex: 1;
    flex-direction: column;
    background: var(variables.$surface);
    padding: var(variables.$spacing-l);
    border-radius: vars.$border-radius-surface;
    border: 1px solid var(variables.$border-on-surface);
    overflow: hidden;

    .rule-form-title-container {
      display: flex;
      justify-content: space-between;
      .form-title {
        @include typography.headline3;
        margin-bottom: var(variables.$spacing-m);
      }
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

  .save-btn {
    width: 150px;
    align-self: flex-end;
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
