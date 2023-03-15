<template>
  <div class="mp-card">
    <div class="first-card-title">Existing Policies</div>

    <div class="row">
      <div class="policy-title">
        {{ policy.name }}
      </div>
      <div
        class="edit"
        @click="$emit('selectPolicy', policy)"
      >
        edit
      </div>
    </div>

    <div v-for="rule in policy.rules">
      <div class="row">
        <div class="title-box rule"><span>Rule: </span>{{ rule.name }}</div>
        <div class="title-box method">
          <span>Detection Method: </span>{{ rule.detectionMethod }}-{{ rule.metricName }}
        </div>
        <div class="title-box component"><span>Component: </span>{{ rule.componentType }}</div>

        <div
          class="alert-conditions-btn"
          @click="triggerRuleState(rule.id)"
        >
          <div>Alert Conditions</div>
          <FeatherIcon :icon="ruleStates[rule.id] ? icons.ExpandLess : icons.ExpandMore" />
        </div>
      </div>

      <div
        class="row"
        v-if="ruleStates[rule.id]"
      >
        <!-- Alert Conditions -->
        <div v-for="(condition, index) in rule.conditions">
          <MonitoringPoliciesCardAlertRow
            :condition="condition"
            :index="index"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { IPolicy } from '@/types/policies'
import ExpandLess from '@featherds/icon/navigation/ExpandLess'
import ExpandMore from '@featherds/icon/navigation/ExpandMore'

const icons = markRaw({
  ExpandLess,
  ExpandMore
})

defineProps<{
  policy: IPolicy
}>()

const ruleStates = reactive<{ [x: string]: boolean }>({})
const triggerRuleState = (ruleId: string) => (ruleStates[ruleId] = !ruleStates[ruleId])
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/mixins/elevation';
@use '@/styles/mediaQueriesMixins.scss';
.mp-card {
  @include elevation.elevation(3);
  border-radius: 5px;
  display: flex;
  gap: var(variables.$spacing-xxs);
  flex-direction: column;
  padding: var(variables.$spacing-l);

  .first-card-title {
    @include typography.headline3;
    margin-bottom: var(variables.$spacing-m);
  }

  .row {
    display: flex;
    gap: var(variables.$spacing-xxs);

    .policy-title {
      @include typography.headline4;
      flex: 1;
    }
    .edit {
      @include typography.subtitle1;
      cursor: pointer;
      color: var(variables.$primary);
      text-decoration: underline;
    }

    .title-box {
      white-space: nowrap;
      background: var(variables.$shade-4);
      padding: var(variables.$spacing-xs);
      border-radius: 3px;
      overflow: hidden;
      text-overflow: ellipsis;
      span {
        display: none;
        @include typography.subtitle2;
      }

      &.rule {
        flex: 1;
      }
      &.method {
        flex: 2;
      }
      &.component {
        flex: 1;
      }
    }

    .alert-conditions-btn {
      @include typography.subtitle2;
      border-radius: 3px;
      white-space: nowrap;
      display: flex;
      justify-content: space-between;
      background: var(variables.$shade-1);
      padding: var(variables.$spacing-xs);
      color: var(variables.$primary-text-on-color);
      width: 145px;
    }
  }

  @include mediaQueriesMixins.screen-md {
    .row {
      .title-box {
        span {
          display: inline;
        }
      }
    }
  }
}
</style>
