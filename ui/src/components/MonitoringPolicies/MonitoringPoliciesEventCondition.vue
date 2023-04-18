<template>
  <div class="condition-title">
    <div class="subtitle">Condition {{ conditionLetters[index].toUpperCase() }}</div>
    <div
      v-if="index !== 0 && !policy.isDefault"
      class="delete"
      @click="$emit('deleteCondition', condition.id)"
    >
      delete
    </div>
  </div>
  <div class="condition">
    <div class="inner-col">
      <div class="text">Trigger Event</div>
      <BasicSelect
        label=""
        hideLabel
        :list="triggerEventOptions"
        @item-selected="(val: number) => updateConditionProp('triggerEvent', val)"
        :disabled="policy.isDefault"
        :selectedId="condition.triggerEvent"
      />
    </div>

    <div class="inner-col">
      <div class="text">Count</div>
      <FeatherInput
        label=""
        hideLabel
        @update:model-value="(val) => updateConditionProp('count', val as number)"
        v-model="condition.count"
        type="number"
        :disabled="policy.isDefault"
      />
    </div>

    <div class="inner-col">
      <div class="text">Over time (Optional)</div>
      <FeatherInput
        label=""
        hideLabel
        @update:model-value="(val) => updateConditionProp('overtime', val as number)"
        v-model="condition.overtime"
        type="number"
        :disabled="policy.isDefault"
      />
    </div>

    <div class="inner-col">
      <div class="text">&nbsp;</div>
      <BasicSelect
        :list="durationOptions"
        @item-selected="(val: number) => updateConditionProp('overtimeUnit', val)"
        :selectedId="condition.overtimeUnit"
        :disabled="policy.isDefault"
      />
    </div>

    <div class="inner-col">
      <div class="text">Severity</div>
      <BasicSelect
        :list="severityList"
        @item-selected="(val: string) => updateConditionProp('severity', val)"
        :selectedId="condition.severity"
        :disabled="policy.isDefault"
      />
    </div>

    <div
      class="inner-col"
      v-if="condition.triggerEvent === SNMPEventType.SNMP_LINK_UP"
    >
      <div class="text">Clear Event (optional)</div>
      <BasicSelect
        :list="clearEventOptions"
        @item-selected="(val: string) => updateConditionProp('clearEvent', val)"
        :selectedId="condition.clearEvent"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import {EventCondition, Policy, Rule} from '@/types/policies'
import {conditionLetters, SNMPEventType, Unknowns} from './monitoringPolicies.constants'
import {Severity, TimeRangeUnit} from '@/types/graphql'

const props = defineProps<{
  condition: EventCondition
  policy: Policy
  rule: Rule
  index: number
}>()

const emit = defineEmits(['updateCondition', 'deleteCondition'])
const condition = ref<EventCondition>()

const durationOptions = [
  { id: Unknowns.UNKNOWN_UNIT, name: '' },
  { id: TimeRangeUnit.Second, name: 'Second(s)' },
  { id: TimeRangeUnit.Minute, name: 'Minute(s)' },
  { id: TimeRangeUnit.Hour, name: 'Hour(s)' }
]

const severityList = [
  { id: Severity.Critical, name: 'Critical' },
  { id: Severity.Major, name: 'Major' },
  { id: Severity.Minor, name: 'Minor' },
  { id: Severity.Warning, name: 'Warning' },
  { id: Severity.Cleared, name: 'Cleared'}
]

const clearEventOptions = [
  { id: Unknowns.UNKNOWN_EVENT, name: '' },
  { id: SNMPEventType.SNMP_LINK_DOWN, name: 'SNMP Link Down'},
]

const triggerEventOptions = [
  { id: SNMPEventType.SNMP_COLD_START, name: 'SNMP Cold Start' },
  { id: SNMPEventType.SNMP_WARM_START, name: 'SNMP Warm Start' },
  { id: SNMPEventType.SNMP_AUTHEN_FAILURE, name: 'SNMP Authentication Failure' },
  { id: SNMPEventType.SNMP_LINK_DOWN, name: 'SNMP Link Down'},
  { id: SNMPEventType.SNMP_LINK_UP, name: 'SNMP Link Up'},
  { id: SNMPEventType.SNMP_EGP_DOWN, name: 'SNMP EGP Down'}
]

watchEffect(() => (condition.value = props.condition))

const updateConditionProp = (property: string, value: number | string) => {
  condition.value![property] = value
  emit('updateCondition', condition)
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins';
@use '@/styles/vars.scss';

.condition {
  display: flex;
  flex-wrap: wrap;
  width: 100%;
  flex-direction: column;
  gap: var(variables.$spacing-xs);

  .inner-col {
    flex: 1;

    .text {
      white-space: nowrap;
      @include typography.caption;
    }

    &.input {
      flex: 0.6;
    }
  }
  @include mediaQueriesMixins.screen-lg {
    flex-direction: row;
  }
}

:deep(.feather-input-sub-text) {
  display: none;
}
</style>
