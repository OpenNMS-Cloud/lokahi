<template>
  <div class="rule-form-container">
    <div>
      <FeatherButton primary @click="ruleFormIsDisplayed = true">
        <FeatherIcon :icon="addIcon" />
        Create New Rule
      </FeatherButton>
      <ExistingItems
        title="Existing Rules"
        :list="[{ name: 'Test'}, { name: 'Testlongername'}]" 
      />
    </div>
    <div class="rule-form" v-if="ruleFormIsDisplayed">
      <div class="form-title">
        Create New Rule
      </div>
      <div class="form-subtitle">
        New Policy Name
      </div>
      <FeatherInput
        v-model="store.selectedRule.name"
        label="New Rule Name"
      />
      <div>
        <div class="subtitle">Component Type</div>
        <BasicSelect
          :list="componentTypes"
          :size="160"
          @item-selected="selectedRuleType"
        />
      </div>

      <DetectionMethod />

      <AlertConditions />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import Add from '@featherds/icon/action/Add'

const store = useMonitoringPoliciesStore()
const addIcon = markRaw(Add)
const ruleFormIsDisplayed = ref(false)

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
@use "@featherds/styles/mixins/elevation";
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.rule-form-container {
  display: flex;
  flex-direction: column;
  gap: var(variables.$spacing-l);

  .rule-form {
    @include elevation.elevation(2);
    display: flex;
    flex: 1;
    flex-direction: column;
    background: var(variables.$surface);
    padding: var(variables.$spacing-l);
    border-radius: 5px;

    .form-title {
      @include typography.headline3;
    }
    .form-subtitle {
      @include typography.subtitle1;
      margin: var(variables.$spacing-m) 0;
    }
  }
}
</style>