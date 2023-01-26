<template>
  <div class="new-rule">
    <div class="title">New Rule</div>
    <div class="form-new-rule">
      <FeatherInput
        v-model="store.selectedRule.name"
        label="New Rule Name"
        class="name-rule"
      />
      <div>
        <div class="subtitle">Component Type</div>
        <BasicSelect
          :list="componentTypes"
          :size="160"
          @item-selected="selectedRuleType"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
const store = useMonitoringPoliciesStore()
const componentTypes = [
  { id: 'cpu', name: 'CPU' },
  { id: 'interface', name: 'Interface' },
  { id: 'storage', name: 'Storage' }
]

const selectedRuleType = (ruleType: string) => {
  store.selectedRule.componentType = ruleType
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.new-rule {
  .title {
    @include typography.headline4;
  }
  .form-new-rule {
    display: flex;
    align-items: flex-end;
    .subtitle {
      @include typography.subtitle2;
    }
    .name-rule {
      width: 350px;
      margin-right: var(variables.$spacing-l);
    }
  }
}

:deep(.feather-input-sub-text) {
  display: none;
}
</style>
