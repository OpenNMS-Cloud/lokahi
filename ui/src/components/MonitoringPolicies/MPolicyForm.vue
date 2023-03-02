<template>
  <div class="policy-form-container">
    <div>
      <FeatherButton primary @click="store.displayPolicyForm()">
        <FeatherIcon :icon="addIcon" />
        Create New Policy
      </FeatherButton>
      <ExistingItems
        title="Existing Policies"
        :list="[{ name: 'Test'}, { name: 'Testlongername'}]" 
      />
    </div>
    <div class="policy-form" v-if="store.selectedPolicy">
      <div class="form-title">
        Create New Policy
      </div>
      <div class="form-subtitle">
        New Policy Name
      </div>
      <FeatherInput
        v-model="store.selectedPolicy.name"
        label="Policy Name"
      />
      <FeatherTextarea
        v-model="store.selectedPolicy.memo"
        label="Memo"
        :maxlength="100"/>
      <FeatherCheckboxGroup
        label="Notifications (Optional)"
        vertical>
        <FeatherCheckbox v-model="store.selectedPolicy.notifications.email">Email</FeatherCheckbox>
        <FeatherCheckbox v-model="store.selectedPolicy.notifications.pagerDuty">Pager Duty</FeatherCheckbox>
        <FeatherCheckbox v-model="store.selectedPolicy.notifications.webhooks">Webhooks</FeatherCheckbox>
      </FeatherCheckboxGroup>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import Add from '@featherds/icon/action/Add'

const store = useMonitoringPoliciesStore()
const addIcon = markRaw(Add)
</script>

<style scoped lang="scss">
@use "@featherds/styles/mixins/elevation";
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.policy-form-container {
  display: flex;
  flex-direction: column;
  gap: var(variables.$spacing-l);

  .policy-form {
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