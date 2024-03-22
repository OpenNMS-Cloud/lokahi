<template>
  <div class="mp-card-alert-container">
    <div class="mp-card-alert-titles">
      <div>&nbsp;</div>
      <div class="col double">Trigger Event</div>
      <div class="col half">Count</div>
      <div class="col half">Over</div>
      <div class="col double">&nbsp;</div>
      <div class="col double">Severity</div>
      <div class="col double">Clear Event</div>
    </div>

    <div class="mp-card-alert-row" v-for="(condition, index) in eventConditions" :key="index">
      <div class="subtitle">{{ conditionLetters[index] + '.' }}</div>
      <div class="col subtitle double">{{ condition.triggerEvent?.name }}</div>
      <div class="col half box">{{ condition.count }}</div>
      <div class="col half box">{{ condition.overtime || '&nbsp;' }}</div>
      <div class="col box double">
        {{
          condition.overtime && condition.overtimeUnit !== Unknowns.UNKNOWN_UNIT
            ? snakeToTitleCase(condition.overtimeUnit as string)
            : '&nbsp;'
        }}
      </div>
      <div class="col severity double"
        :class="`${condition.severity!.toLowerCase()}-color`">
        {{ snakeToTitleCase(condition.severity) }}
      </div>
      <div class="col box double">
        {{ condition.clearEvent?.name || '&nbsp;' }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { conditionLetters, Unknowns } from './monitoringPoliciesLegacy.constants'
import { snakeToTitleCase } from '../utils'
import { AlertCondition } from '@/types/graphql'

defineProps<{
  eventConditions: AlertCondition[]
}>()
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/vars.scss';
@use '@/styles/_severities.scss';

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
