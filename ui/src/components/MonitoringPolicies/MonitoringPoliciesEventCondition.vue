<template>
  <div class="condition-title">
    <div class="subtitle">Condition {{ conditionLetters[index].toUpperCase() }}</div>
    <div
      v-if="index !== 0"
      class="delete"
      @click="$emit('deleteCondition', condition.id)"
    >
      delete
    </div>
  </div>
  <div class="condition">
    <div class="inner-col">
      <div class="text">Count</div>
      <FeatherInput
        label=""
        hideLabel
        @update:model-value="(val) => updateConditionProp('count', val as number)"
        v-model="condition.count"
        type="number"
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
      />
    </div>

    <div class="inner-col">
      <div class="text">&nbsp;</div>
      <BasicSelect
        :list="durationOptions"
        @item-selected="(val: number) => updateConditionProp('overtimeUnit', val)"
        :selectedId="condition.overtimeUnit"
      />
    </div>

    <div class="inner-col">
      <div class="text">Severity</div>
      <BasicSelect
        :list="severityList"
        @item-selected="(val: string) => updateConditionProp('severity', val)"
        :selectedId="condition.severity"
      />
    </div>

    <div
      class="inner-col"
      v-if="rule.triggerEvent === SNMPEventType.PORT_DOWN || condition.triggerEvent === SNMPEventType.PORT_DOWN"
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
import { EventCondition, Rule } from '@/types/policies'
import { conditionLetters } from './monitoringPolicies.constants'
import { OverTimeUnit, Severity, SNMPEventType } from './monitoringPolicies.constants'

const props = defineProps<{
  condition: EventCondition
  rule: Rule
  index: number
}>()

const emit = defineEmits(['updateCondition', 'deleteCondition'])
const condition = ref<EventCondition>()

const durationOptions = [
  { id: OverTimeUnit.SECOND, name: 'Second(s)' },
  { id: OverTimeUnit.MINUTE, name: 'Minute(s)' },
  { id: OverTimeUnit.HOUR, name: 'Hour(s)' }
]

const severityList = [
  { id: Severity.CRITICAL, name: 'Critical' },
  { id: Severity.MAJOR, name: 'Major' },
  { id: Severity.MINOR, name: 'Minor' },
  { id: Severity.WARNING, name: 'Warning' }
]

const clearEventOptions = [
  { id: undefined, name: '' },
  { id: SNMPEventType.PORT_UP, name: 'Port Up' }
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
