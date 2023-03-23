<template>
  <div class="mp-card">
    <div
      class="first-card-title"
      v-if="index === 0"
    >
      Existing Policies
    </div>

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
        <div class="title-box rule">{{ rule.name }}</div>
        <div class="title-box method">{{ rule.detectionMethod }}-{{ rule.metricName }}</div>
        <div class="title-box component">{{ rule.componentType }}</div>

        <div
          class="alert-conditions-btn"
          @click="triggerRuleState(rule.id)"
        >
          <div>Alert Conditions</div>
          <FeatherIcon :icon="ruleStates[rule.id] ? icons.ExpandLess : icons.ExpandMore" />
        </div>
      </div>

      <div v-if="ruleStates[rule.id]">
        <div class="alert-title">Alert Conditions</div>
        <MonitoringPoliciesCardAlertRow
          v-for="(condition, index) in rule.conditions"
          :rule="rule"
          :condition="condition"
          :index="index"
        />
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

const props = defineProps<{
  policy: IPolicy
  index: number
}>()

const ruleStates = reactive<{ [x: string]: boolean }>({})
const triggerRuleState = (ruleId: string) => (ruleStates[ruleId] = !ruleStates[ruleId])

// set first rule alert conditions open by default
onMounted(() => ruleStates[props.policy.rules[0].id] = true)
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/mixins/elevation';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';

.mp-card {
  @include elevation.elevation(3);
  border-radius: vars.$border-radius-s;
  display: flex;
  gap: var(variables.$spacing-xl);
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
      border-radius: vars.$border-radius-xs;
      overflow: hidden;
      text-overflow: ellipsis;
      span {
        display: none;
        @include typography.subtitle2;
      }

      &.rule {
        @include typography.subtitle2;
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
      border-radius: vars.$border-radius-xs;
      white-space: nowrap;
      display: flex;
      justify-content: space-between;
      background: var(variables.$shade-1);
      padding: var(variables.$spacing-xs);
      color: var(variables.$primary-text-on-color);
      width: 145px;
      cursor: pointer;
    }
  }

  .alert-title {
    @include typography.subtitle1;
    margin-top: var(variables.$spacing-xl);
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
