<template>
  <div class="alert-conditions">
    <div class="title">Set Alert Conditions</div>
    <FeatherButton
      primary
      @click="addComponent"
      class="btn"
    >
      <FeatherIcon
        :icon="Add"
        class="icon"
      />
      Add
    </FeatherButton>
    <div v-if="store.selectedRule.conditions.length">
      <div
        class="condition-card"
        v-for="cond in store.selectedRule.conditions"
        :key="cond.id"
      >
        <Condition :condition="cond" />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import Add from '@featherds/icon/action/Add'
const store = useMonitoringPoliciesStore()

const addComponent = () => {
  store.addNewCondition()
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.alert-conditions {
  margin-top: var(variables.$spacing-xl);
  .btn {
    margin-top: var(variables.$spacing-s);
  }
  .title {
    @include typography.headline4;
  }
  .icon {
    font-size: 20px;
    vertical-align: text-bottom;
  }
  .condition-card {
    border-bottom: 1px solid var(variables.$shade-4);
    &:last-child {
      border: none;
    }
  }
}
</style>
