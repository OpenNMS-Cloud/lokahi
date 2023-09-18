<template>
  <div
    class="btns"
    v-if="store.selectedPolicy && !store.selectedPolicy.isDefault"
  >
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
  () => store.selectedPolicy?.isDefault || !store.selectedPolicy?.rules?.length || !store.selectedPolicy.name
)
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
.btns {
  display: flex;
  flex-direction: column;
  margin-bottom: var(variables.$spacing-xl);

  .save-btn {
    align-self: flex-end;
  }
}
</style>
