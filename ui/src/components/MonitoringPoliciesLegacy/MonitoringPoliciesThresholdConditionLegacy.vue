<template>
  <div class="condition-title">
    <div class="subtitle">Condition {{ conditionLetters[index].toUpperCase() }}</div>
    <div
      v-if="index !== 0"
      class="delete"
      @click="$emit('deleteCondition', alertCondition.id)"
    >
      Delete condition {{ conditionLetters[index].toUpperCase() }}
    </div>
  </div>
  <div
    class="condition"
    v-if="alertCondition"
  >
    <div class="inner-col">
      <div class="text">Trigger when metric is:</div>
      <BasicSelect
        :list="levelOptions"
        @item-selected="(val: string) => {
          alertCondition.level = val
          $emit('updateCondition', alertCondition)
        }"
        :selectedId="alertCondition.level"
      />
    </div>

    <div class="inner-col slider">
      <div class="text">Severity threshold %</div>
      <Slider
        v-model="alertCondition.percentage"
        :min="0"
        :max="100"
        :tooltipPosition="'bottom'"
        :format="(num: number) => num + '%'"
        @change="(val: number) => {
          alertCondition.percentage = val
          $emit('updateCondition', alertCondition)
        }"
      />
    </div>

    <div class="inner-col input">
      <div class="text">For any</div>
      <FeatherInput
        label=""
        hideLabel
        @update:model-value="() => $emit('updateCondition', alertCondition)"
        v-model="alertCondition.forAny"
      />
    </div>

    <div class="inner-col">
      <div class="text">&nbsp;</div>
      <BasicSelect
        :list="durationOptions"
        @item-selected="(val: string) => {
          alertCondition.durationUnit = val
          $emit('updateCondition', alertCondition)
        }"
        :selectedId="alertCondition.durationUnit"
      />
    </div>

    <div class="inner-col input">
      <div class="text">During the last</div>
      <FeatherInput
        label=""
        hideLabel
        @update:model-value="() => $emit('updateCondition', alertCondition)"
        v-model="alertCondition.duringLast"
      />
    </div>

    <div class="inner-col">
      <div class="text">&nbsp;</div>
      <BasicSelect
        :list="durationOptions"
        @item-selected="(val: string) => {
          alertCondition.periodUnit = val
          $emit('updateCondition', alertCondition)
        }"
        :selectedId="alertCondition.periodUnit"
      />
    </div>

    <div class="inner-col">
      <div class="text">As</div>
      <BasicSelect
        :list="severityList"
        @item-selected="(val: string) => {
          alertCondition.severity = val
          $emit('updateCondition', alertCondition)
        }"
        :selectedId="alertCondition.severity"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import Slider from '@vueform/slider'
import { Severity, TimeRangeUnit } from '@/types/graphql'
import { ThresholdCondition } from '@/types/policies'
import { conditionLetters, ThresholdLevels, Unknowns } from './monitoringPoliciesLegacy.constants'

const props = defineProps<{
  condition: ThresholdCondition
  index: number
}>()

defineEmits<{
  (e: 'updateCondition', alertCondition: ThresholdCondition): void,
  (e: 'deleteCondition', id: string): void
}>()

const alertCondition = ref<ThresholdCondition>(props.condition)

const levelOptions = [
  { id: ThresholdLevels.ABOVE, name: 'above' },
  { id: ThresholdLevels.EQUAL_TO, name: 'equal to' },
  { id: ThresholdLevels.BELOW, name: 'below' },
  { id: ThresholdLevels.NOT_EQUAL_TO, name: 'is not equal' }
]

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
  { id: Severity.Warning, name: 'Warning' }
]

watchEffect(() => (alertCondition.value = props.condition))
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

    &.slider {
      display: flex;
      align-items: center;
      flex-direction: column;
      gap: var(variables.$spacing-s);
    }

    &.input {
      flex: 0.6;
    }
  }
  @include mediaQueriesMixins.screen-lg {
    flex-direction: row;
  }
}

:root {
  .slider-horizontal {
    width: 100%;
    --slider-tooltip-distance: 10px;
    --slider-tooltip-bg: #273180;
    --slider-connect-bg: #273180;
  }
}

:deep(.feather-input-sub-text) {
  display: none;
}
</style>

<style src="@vueform/slider/themes/default.css"></style>
