<template>
  <div
    class="mp-card-alert-row"
    v-if="isThresholdCondition(condition)"
  >
    <div class="subtitle">{{ conditionLetters[index] + '.' }}</div>
    <div class="col tripple">Trigger when the metric is:</div>
    <div class="col box">{{ condition.level }}</div>
    <div class="col box half">{{ condition.percentage }}</div>
    <div class="col">for any</div>
    <div class="col box half">{{ condition.forAny }}</div>
    <div class="col box double">{{ condition.durationUnit }}</div>
    <div class="col double">during the last</div>
    <div class="col box half">{{ condition.duringLast }}</div>
    <div class="col box double">{{ condition.periodUnit }}</div>
    <div class="col half">as</div>
    <div class="col box double">{{ condition.severity }}</div>
  </div>

  <div
    v-else
    class="mp-card-alert-container"
  >
    <div
      class="mp-card-alert-titles"
      v-if="index === 0"
    >
      <div>&nbsp</div>
      <div class="col double">Trigger Event</div>
      <div class="col half">Count</div>
      <div class="col half">Over</div>
      <div class="col double">&nbsp</div>
      <div class="col double">Severity</div>
      <div
        class="col double"
        v-if="isEventPortDownCondition(condition)"
      >
        Clear Event
      </div>
    </div>

    <div class="mp-card-alert-row">
      <div class="subtitle">{{ conditionLetters[index] + '.' }}</div>
      <div class="col subtitle double">{{ rule.eventTrigger }}</div>
      <div class="col half box">{{ condition.count }}</div>
      <div class="col half box">{{ condition.time || '&nbsp' }}</div>
      <div class="col box double">{{ condition.unit || '&nbsp' }}</div>
      <div class="col box double">{{ condition.severity }}</div>
      <div
        class="col box double"
        v-if="isEventPortDownCondition(condition)"
      >
        {{ condition.clearEvent }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Condition, IRule } from '@/types/policies'
import { isThresholdCondition, isEventPortDownCondition } from './monitoringPolicies.utils'
import { conditionLetters } from './monitoringPolicies.constants'

defineProps<{
  rule: IRule
  condition: Condition
  index: number
}>()
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/mixins/elevation';
@use '@/styles/vars.scss';

.mp-card-alert-container {
  display: flex;
  flex-direction: column;

  .mp-card-alert-titles {
    @include typography.body-small;
    display: flex;
    width: 100%;
    gap: var(variables.$spacing-xxs);
    background: var(variables.$shade-4);
    margin-top: var(variables.$spacing-s);
    padding: var(variables.$spacing-xxs) 0;
  }
}

.mp-card-alert-row {
  display: flex;
  width: 100%;
  gap: var(variables.$spacing-xxs);
  @include typography.caption;
  margin-top: var(variables.$spacing-m);
  align-items: center;
}

.col {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  text-align: center;

  &.half {
    flex: 0.5;
  }
  &.double {
    flex: 2;
  }
  &.tripple {
    flex: 3;
  }
}

.box {
  @include typography.subtitle1;
  background: var(variables.$shade-4);
  padding: var(variables.$spacing-xs);
}

.subtitle {
  @include typography.subtitle1;
}
</style>
