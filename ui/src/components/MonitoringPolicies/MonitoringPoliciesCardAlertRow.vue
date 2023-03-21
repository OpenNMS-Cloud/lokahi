<template>
  <div class="mp-card-alert-row" v-if="isThresholdCondition(condition)">
    <div class="alert-letter">{{ conditionLetters[index] }}</div>
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

  <div class="mp-card-alert-row" v-else>
    <div class="alert-letter">{{ conditionLetters[index] }}</div>
    <div class="col tripple">Trigger event at:</div>
    <div class="col box">{{ condition.count }}</div>
    <div class="col">occurances</div>
    <div class="col" v-if="condition.time">over</div>
    <div class="col box half" v-if="condition.time">{{ condition.time }}</div>
    <div class="col box double" v-if="condition.time">{{ condition.unit }}</div>
    <div class="col half">as</div>
    <div class="col box double">{{ condition.severity }}</div>
    <div class="col tripple" v-if="isEventPortDownCondition(condition)">Clear event when:</div>
    <div class="col box double" v-if="isEventPortDownCondition(condition)">{{ condition.clearEvent }}</div>
  </div>
</template>

<script setup lang="ts">
import { Condition } from '@/types/policies'
import { isThresholdCondition, isEventPortDownCondition } from './monitoringPolicies.utils'

const conditionLetters = ['a.', 'b.', 'c.', 'd.']

defineProps<{
  condition: Condition
  index: number
}>()
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/mixins/elevation';
@use '@/styles/vars.scss';

.mp-card-alert-row {
  display: flex;
  width: 100%;
  gap: var(variables.$spacing-xxs);
  @include typography.caption;
  margin: var(variables.$spacing-m) 0;
  align-items: center;


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

  .alert-letter {
    @include typography.subtitle1;
    display: flex;
    align-items: center;
    height: 30px;
    width: 30px;
    background: var(variables.$primary);
    padding: var(variables.$spacing-xs);
    color: var(variables.$primary-text-on-color);
    border-radius: vars.$border-radius-round;
  }
  .box {
    @include typography.subtitle1;
    background: var(variables.$shade-4);
    padding: var(variables.$spacing-xs);
  }
}
</style>
