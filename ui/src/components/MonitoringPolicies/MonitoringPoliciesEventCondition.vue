<template>
  <div class="condition-title">
    <div class="subtitle">Condition {{ conditionLetters[index].toUpperCase() }}</div>
    <div
      v-if="index !== 0 && !policy.isDefault"
      class="delete"
      @click="$emit('deleteCondition', alertCondition.id)"
    >
      Delete condition {{ conditionLetters[index].toUpperCase() }}
    </div>
  </div>
  <div class="condition">
    <div class="inner-col">
      <div class="text">Trigger Event</div>
      <FeatherSelect
        label="Trigger Event"
        hideLabel
        :options="triggerEventDefinitionOptions"
        text-prop="name"
        v-model="alertCondition.triggerEvent"
        :disabled="policy.isDefault"
        @update:modelValue="$emit('updateCondition', alertCondition)"
      />
    </div>

    <div class="inner-col">
      <div class="text">Count</div>
      <FeatherInput
        label=""
        hideLabel
        v-model="alertCondition.count"
        type="number"
        :disabled="policy.isDefault"
        @update:modelValue="$emit('updateCondition', alertCondition)"
      />
    </div>

    <div class="inner-col">
      <div class="text">Over time (Optional)</div>
      <FeatherInput
        label=""
        hideLabel
        v-model="alertCondition.overtime"
        type="number"
        :disabled="policy.isDefault"
        @update:modelValue="$emit('updateCondition', alertCondition)"
      />
    </div>

    <div class="inner-col">
      <div class="text">&nbsp;</div>
      <BasicSelect
        :list="durationOptions"
        @item-selected="(val: string) => {
          alertCondition.overtimeUnit = val
          $emit('updateCondition', alertCondition)
        }"
        :selectedId="alertCondition.overtimeUnit"
        :disabled="policy.isDefault"
      />
    </div>

    <div class="inner-col">
      <div class="text">Severity</div>
      <BasicSelect
        :list="severityOptions"
        @item-selected="(val: string) => {
          alertCondition.severity = val
          $emit('updateCondition', alertCondition)
        }"
        :selectedId="alertCondition.severity"
        :disabled="policy.isDefault"
      />
    </div>

    <div class="inner-col">
      <div class="text">Clear Event (optional)</div>
      <FeatherSelect
        label=""
        hideLabel
        :options="clearEventDefinitionOptions"
        text-prop="name"
        @update:modelValue="$emit('updateCondition', alertCondition)"
        v-model="alertCondition.clearEvent"
        :disabled="policy.isDefault"
        clear="Remove"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import {EventCondition, Policy, Rule} from '@/types/policies'
import {conditionLetters, Unknowns} from './monitoringPolicies.constants'
import { EventType, Severity, TimeRangeUnit } from '@/types/graphql'
import { Ref } from 'vue'
import { ISelectItemType } from '@featherds/select'
import { useAlertEventDefinitionQueries } from '@/store/Queries/alertEventDefinitionQueries'
import useSnackbar from '@/composables/useSnackbar'

const { showSnackbar } = useSnackbar()

const props = defineProps<{
  condition: EventCondition
  policy: Policy
  rule: Rule
  eventType: EventType
  index: number
}>()

const alertEventDefinitionStore = useAlertEventDefinitionQueries()

const emit = defineEmits(['updateCondition', 'deleteCondition'])

const alertCondition = ref(props.condition)

const durationOptions = [
  { id: Unknowns.UNKNOWN_UNIT, name: '' },
  { id: TimeRangeUnit.Second, name: 'Second(s)' },
  { id: TimeRangeUnit.Minute, name: 'Minute(s)' },
  { id: TimeRangeUnit.Hour, name: 'Hour(s)' }
]

const severityOptions = [
  { id: Severity.Critical, name: 'Critical' },
  { id: Severity.Major, name: 'Major' },
  { id: Severity.Minor, name: 'Minor' },
  { id: Severity.Warning, name: 'Warning' },
  { id: Severity.Cleared, name: 'Cleared'}
]

let clearEventDefinitionOptions: Ref<ISelectItemType[]> = ref([])
let triggerEventDefinitionOptions: Ref<ISelectItemType[]> = ref([])

onMounted(async () => {
  try {
    const result = await alertEventDefinitionStore.listAlertEventDefinitions(props.eventType)

    clearEventDefinitionOptions.value = result.value?.listAlertEventDefinitions || []
    triggerEventDefinitionOptions.value = result.value?.listAlertEventDefinitions || []
  } catch (err) {
    showSnackbar({
      msg: 'Failed to load selectable events.'
    })
  }
})

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
