<template>
  <div class="condition-title">
    <div class="subtitle">Condition {{ conditionLetters[index].toUpperCase() }}</div>
    <div
      v-if="index !== 0"
      class="delete"
      @click="$emit('deleteCondition', condition.id)"
    >
      Delete condition {{ conditionLetters[index].toUpperCase() }}
    </div>
  </div>
  <div
    class="condition"
    v-if="condition"
  >
    <div class="inner-col">
      <div class="text">Trigger when metric is:</div>
      <BasicSelect
        :list="levelOptions"
        @item-selected="(val:string) => updateConditionProp('level', val)"
        :selectedId="condition.level"
      />
    </div>

    <div class="inner-col slider">
      <div class="text">Severity threshold %</div>
      <Slider
        v-model="condition.percentage"
        :min="0"
        :max="100"
        :tooltipPosition="'bottom'"
        :format="(num: number) => num + '%'"
        @change="(val:number) => updateConditionProp('percentage', val)"
      />
    </div>

    <div class="inner-col input">
      <div class="text">For any</div>
      <FeatherInput
        label=""
        hideLabel
        @update:model-value="(val) => updateConditionProp('forAny', val as string)"
        v-model="condition.forAny"
      />
    </div>

    <div class="inner-col">
      <div class="text">&nbsp;</div>
      <BasicSelect
        :list="durationOptions"
        @item-selected="(val:number) => updateConditionProp('duration', val)"
        :selectedId="condition.durationUnit"
      />
    </div>

    <div class="inner-col input">
      <div class="text">During the last</div>
      <FeatherInput
        label=""
        hideLabel
        @update:model-value="(val) => updateConditionProp('period', val as string)"
        v-model="condition.duringLast"
      />
    </div>

    <div class="inner-col">
      <div class="text">&nbsp;</div>
      <BasicSelect
        :list="durationOptions"
        @item-selected="(val:number) => updateConditionProp('duringLast', val)"
        :selectedId="condition.periodUnit"
      />
    </div>

    <div class="inner-col">
      <div class="text">As</div>
      <BasicSelect
        :list="severityList"
        @item-selected="(val:string) => updateConditionProp('severity', val)"
        :selectedId="condition.severity"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import Slider from '@vueform/slider'
import { ThresholdCondition } from '@/types/policies'
import { conditionLetters, ThresholdLevels, Unknowns } from './monitoringPolicies.constants'
import { Severity, TimeRangeUnit } from '@/types/graphql'

const props = defineProps<{
  condition: ThresholdCondition
  index: number
}>()

const emit = defineEmits(['updateCondition', 'deleteCondition'])
const condition = ref<ThresholdCondition>()

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
