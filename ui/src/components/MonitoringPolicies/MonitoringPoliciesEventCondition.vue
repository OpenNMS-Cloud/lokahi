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
        @update:model-value="(val) => updateConditionProp('time', val as number)"
        v-model="condition.time"
        type="number"
      />
    </div>

    <div class="inner-col">
      <div class="text">&nbsp;</div>
      <BasicSelect
        :list="durationOptions"
        @item-selected="(val:number) => updateConditionProp('unit', val)"
        :selectedId="condition.unit"
      />
    </div>

    <div class="inner-col">
      <div class="text">Severity</div>
      <BasicSelect
        :list="severityList"
        @item-selected="(val:string) => updateConditionProp('severity', val)"
        :selectedId="condition.severity"
      />
    </div>

    <div
      class="inner-col"
      v-if="isEventPortDownCondition(condition)"
    >
      <div class="text">Clear Event (optional)</div>
      <BasicSelect
        :list="clearEventOptions"
        @item-selected="(val:string) => updateConditionProp('clearEvent', val)"
        :selectedId="condition.clearEvent"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { EventCondition } from '@/types/policies'
import { isEventPortDownCondition } from './monitoringPolicies.utils'
import { conditionLetters } from './monitoringPolicies.constants'

const props = defineProps<{
  condition: EventCondition
  index: number
}>()

const emit = defineEmits(['updateCondition'])
const condition = ref<EventCondition>()

const durationOptions = [
  { id: 'second', name: 'Second(s)' },
  { id: 'minute', name: 'Minute(s)' },
  { id: 'hour', name: 'Hour(s)' },
  { id: 'day', name: 'Day(s)' }
]

const severityList = [
  { id: 'critical', name: 'Critical' },
  { id: 'major', name: 'Major' },
  { id: 'minor', name: 'Minor' },
  { id: 'warning', name: 'Warning' }
]

const clearEventOptions = [
  { id: null, name: '' },
  { id: 'port-up', name: 'Port Up' }
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
