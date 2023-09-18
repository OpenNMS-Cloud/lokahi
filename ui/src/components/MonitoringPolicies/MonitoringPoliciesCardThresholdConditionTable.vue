<template>
  <div class="mp-card-alert-row" v-for="(condition, index) in thresholdConditions" :key="index">
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
    <div class="col severity double" :class="`${condition.severity}-color`" >
      {{ condition.severity }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ThresholdCondition } from '@/types/policies'
import { conditionLetters } from './monitoringPolicies.constants'

defineProps<{
  thresholdConditions: ThresholdCondition[]
}>()
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/vars.scss';
@use '@/styles/_severities.scss';

.mp-card-alert-row {
  display: flex;
  flex-wrap: wrap;
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

.box,
.severity {
  @include typography.subtitle1;
  padding: var(variables.$spacing-xs);
  border-radius: vars.$border-radius-s;
}
.box {
  background: var(variables.$shade-4);
}

.subtitle {
  @include typography.subtitle1;
}
</style>
