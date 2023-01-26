<template>
  <div class="condition">
    <div class="text">Trigger when the metric is</div>
    <BasicSelect
      :list="limitList"
      :size="160"
      @item-selected="(val:string) => setValue('level', val)"
      :isDisabled="!isEdit"
    />
    <Slider
      v-model="percentage"
      :min="0"
      :max="100"
      class="slider-red"
      :tooltipPosition="'top'"
      :format="(num: number) => num + '%'"
      @change="(val:number) => setValue('percentage', val)"
      :disabled="!isEdit"
    />
    <div class="text">for any</div>
    <BasicSelect
      :list="timeList"
      :size="180"
      @item-selected="(val:number) => setValue('duration', val)"
      :isDisabled="!isEdit"
    />
    <div class="text">during the last</div>
    <BasicSelect
      :list="lastPeriodList"
      :size="180"
      @item-selected="(val:number) => setValue('period', val)"
      :isDisabled="!isEdit"
    />
    <div class="text">as</div>
    <BasicSelect
      :list="severityList"
      :size="180"
      @item-selected="(val:string) => setValue('severity', val)"
      :isDisabled="!isEdit"
    />
    <div class="icons">
      <div class="action">
        <FeatherIcon
          v-if="isEdit"
          @click="saveCondition"
          :icon="Icons.Save"
          class="icon edit"
        />
        <FeatherIcon
          v-else
          @click="isEdit = true"
          :icon="Icons.EditMode"
          class="icon edit"
        />
      </div>
      <FeatherIcon
        @click="remove"
        :icon="Icons.Delete"
        class="icon delete"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import Slider from '@vueform/slider'
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import Delete from '@featherds/icon/action/Delete'
import EditMode from '@featherds/icon/action/EditMode'
import Save from '@featherds/icon/action/MarkComplete'
import { ICondition } from '@/types/policies'
import type { Ref } from 'vue'

import { markRaw } from 'vue'
const Icons = markRaw({
  Delete,
  EditMode,
  Save
})
const store = useMonitoringPoliciesStore()

const props = defineProps<{
  condition: ICondition
}>()
const isEdit = ref(true)
const percentage = ref(props.condition.percentage)
const alert = ref(props.condition) as Ref<ICondition>

const limitList = [
  { id: 'above', name: 'above' },
  { id: 'equalTo', name: 'equalTo' },
  { id: 'below', name: 'below' },
  { id: 'isNotEqual', name: 'is not equal' }
]

//time durations
const keys = Array.from({ length: 12 }, (v, i) => 5 + i * 5)
const timeList = keys.map((key) => {
  return { id: key, name: key + ' minutes' }
})

//last event time
const keysSteps = Array.from({ length: 10 }, (v, i) => 15 + i * 15)
const lastPeriodList = keysSteps.map((key) => {
  return { id: key, name: key + ' minutes' }
})

//severities
const severityList = [
  { id: 'critical', name: 'Critical' },
  { id: 'major', name: 'Major' },
  { id: 'minor', name: 'Minor' },
  { id: 'warning', name: 'Warning' }
]

const setValue = (property: string, value: number | string) => {
  alert.value[property] = value
}

const remove = () => {
  store.removeCondition(props.condition.id)
}

const saveCondition = () => {
  isEdit.value = false
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.condition {
  display: flex;
  align-items: center;
  padding: var(variables.$spacing-l) var(variables.$spacing-s);
  margin-top: var(variables.$spacing-s);

  > * {
    margin: 0 var(variables.$spacing-s);
    &:first-child {
      margin: 0;
    }
  }

  > .text {
    font-weight: 600;
  }

  .icons {
    display: flex;
    justify-content: flex-end;
    flex-grow: 1;
    margin-right: 0;
    .action {
      display: flex;
      align-items: center;
    }
    .icon {
      font-size: 26px;
      margin-left: 10px;
      cursor: pointer;

      &.delete {
        color: var(variables.$error);
      }
      &.edit {
        color: var(variables.$success);
      }
    }
  }
}

.slider {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  width: 100%;
  align-items: center;
}
:root {
  .slider-horizontal {
    width: 200px;
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
