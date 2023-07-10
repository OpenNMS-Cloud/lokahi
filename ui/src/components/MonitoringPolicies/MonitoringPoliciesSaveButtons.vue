<template>
  <div
    class="btns"
    v-if="store.selectedPolicy && store.selectedRule"
  >
    <FeatherButton
      class="save-btn"
      primary
      @click="store.saveRule"
      :disabled="disableSaveRuleBtn"
      data-test="save-rule-btn"
    >
      Save Rule
    </FeatherButton>
    <hr />
    <ButtonWithSpinner
      :isFetching="mutations.isFetching.value"
      class="save-btn"
      primary
      :disabled="disableSavePolicyBtn"
      @click="store.savePolicy"
      data-test="save-policy-btn"
    >
      Save Policy
    </ButtonWithSpinner>
  </div>
</template>

<script setup lang="ts">
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import { useMonitoringPoliciesMutations } from '@/store/Mutations/monitoringPoliciesMutations'

const store = useMonitoringPoliciesStore()
const mutations = useMonitoringPoliciesMutations()
const disableSavePolicyBtn = computed(
  () => store.selectedPolicy?.isDefault || !store.selectedPolicy?.rules.length || !store.selectedPolicy.name
)
const disableSaveRuleBtn = computed(
  () => store.selectedPolicy?.isDefault || !store.selectedRule?.name || !store.selectedRule?.alertConditions.length
)
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
.btns {
  display: flex;
  flex-direction: column;
  margin: var(variables.$spacing-xl) 0;

  .save-btn {
    width: 150px;
    align-self: flex-end;
  }
}
</style>
