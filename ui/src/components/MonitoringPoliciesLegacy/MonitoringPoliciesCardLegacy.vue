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
      <FeatherButton
        icon="Edit"
        class="edit"
        @click="emit('selectPolicy', policy)"
        data-test="policy-edit-btn"
        v-if="!policy.isDefault"
      >
        <FeatherIcon :icon="icons.Edit" />
      </FeatherButton>
      <FeatherButton
        icon="Copy"
        class="copy"
        @click="emit('copyPolicy', policy)"
        data-test="policy-copy-btn"
      >
        <FeatherIcon :icon="icons.ContentCopy" />
      </FeatherButton>
      <FeatherButton
        icon="Delete"
        class="delete"
        @click="emit('deletePolicy', policy)"
        data-test="policy-delete-btn"
        v-if="!policy.isDefault"
      >
        <FeatherIcon :icon="icons.Delete" />
      </FeatherButton>
    </div>

    <div
      v-for="rule in policy.rules"
      :key="rule.id"
    >
      <div class="row">
        <div class="title-box rule">{{ rule.name }}</div>
        <!-- BE does not have these props yet -->
        <!-- <div class="title-box method">{{ rule.detectionMethod }}-{{ rule.metricName }}</div> -->
        <div class="title-box component">{{ rule.componentType }}</div>

        <div
          class="alert-conditions-btn"
          @click="triggerRuleState(rule.id)"
        >
          <div>Alert Conditions</div>
          <FeatherIcon
            class="expand-icon"
            :icon="ruleStates[rule.id] ? icons.ExpandLess : icons.ExpandMore"
          />
        </div>
      </div>

      <div v-if="ruleStates[rule.id]">
        <div class="alert-title">Alert Conditions</div>
        <MonitoringPoliciesCardEventConditionTableLegacy
          :eventConditions="rule.alertConditions as AlertCondition[]"
        />
<!--    TODO: Bring this back when thresholding is implemented
          https://opennms.atlassian.net/browse/HS-750
        <MonitoringPoliciesCardEventConditionTableLegacy
          v-if="rule.detectionMethod === DetectionMethod.EVENT"
          :eventConditions="rule.alertConditions"
        />
        <MonitoringPoliciesCardThresholdConditionTableLegacy
          v-else-if="rule.detectionMethod === DetectionMethod.THRESHOLD"
          :thresholdConditions="[]"
        />-->
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Policy } from '@/types/policies'
import ExpandLess from '@featherds/icon/navigation/ExpandLess'
import ExpandMore from '@featherds/icon/navigation/ExpandMore'
import ContentCopy from '@featherds/icon/action/ContentCopy'
import Delete from '@featherds/icon/action/Delete'
import Edit from '@featherds/icon/action/Edit'
import { AlertCondition } from '@/types/graphql'

const icons = markRaw({
  ExpandLess,
  ExpandMore,
  ContentCopy,
  Edit,
  Delete
})

const props = defineProps<{
  policy: Policy
  index: number
}>()

const emit = defineEmits<{
  (e: 'copyPolicy', policy: Policy): void,
  (e: 'deletePolicy', policy: Policy): void,
  (e: 'selectPolicy', policy: Policy): void
}>()

const ruleStates = reactive<{ [x: string]: boolean }>({})
const triggerRuleState = (ruleId: string) => (ruleStates[ruleId] = !ruleStates[ruleId])

// set first rule alert conditions open by default
onMounted(() => (ruleStates[props.policy.rules?.[0].id] = true))
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';

.mp-card {
  background: var(variables.$surface);
  border-radius: vars.$border-radius-surface;
  border: 1px solid var(variables.$border-on-surface);
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
    flex-wrap: wrap;
    gap: var(variables.$spacing-xxs);

    .policy-title {
      @include typography.headline4;
      flex: 1;
    }
    .delete,
    .edit,
    .copy {
      @include typography.subtitle1;
      cursor: pointer;
      color: var(variables.$primary);
      text-decoration: underline;
    }

    .copy {
      margin-left: var(variables.$spacing-xs);
    }

    .title-box {
      white-space: nowrap;
      background: var(variables.$shade-4);
      padding: var(variables.$spacing-xs);
      border-radius: vars.$border-radius-s;
      overflow: hidden;
      text-overflow: ellipsis;
      span {
        display: none;
        @include typography.subtitle2;
      }

      &.rule {
        @include typography.subtitle2;
        line-height: 2;
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
      border-radius: vars.$border-radius-s;
      white-space: nowrap;
      display: flex;
      justify-content: space-between;
      background: var(variables.$shade-1);
      padding: var(variables.$spacing-s);
      color: var(variables.$primary-text-on-color);
      width: 145px;
      cursor: pointer;
    }

    .expand-icon {
      font-size: 20px;
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
