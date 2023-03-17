<template>
  <div class="mp-card-alert-row">
    <div class="alert-letter">{{ conditionLetters[index] }}</div>
    <div class="col trigger">Trigger when the metric is:</div>
    <div class="col box">{{ condition.level }}</div>
    <div class="col box">{{ condition.percentage }}</div>
    <div class="col">for any</div>
    <div class="col box">{{ condition.duration }}</div>
    <div class="col period">during the last</div>
    <div class="col box">{{ condition.period }}</div>
    <div class="col">as</div>
    <div class="col box">{{ condition.severity }}</div>
  </div>
</template>

<script setup lang="ts">
import { ICondition } from '@/types/policies'

const conditionLetters = ['a.', 'b.', 'c.', 'd.']

defineProps<{
  condition: ICondition
  index: number
}>()
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/mixins/elevation';
.mp-card-alert-row {
  display: flex;
  width: 100%;
  gap: var(variables.$spacing-xxs);
  @include typography.body-large;
  margin: var(variables.$spacing-m) 0;
  align-items: center;


  .col {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    flex: 1;
    text-align: center;
    &.trigger {
      flex: 3;
    }
    &.period {
      flex: 2;
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
    border-radius: 50%;
  }
  .box {
    @include typography.subtitle1;
    background: var(variables.$shade-4);
    padding: var(variables.$spacing-xs);
  }
}
</style>
