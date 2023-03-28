<template>
  <div
    class="btns"
    v-if="store.selectedPolicy && store.selectedRule"
  >
    <FeatherButton
      class="save-btn"
      primary
      :disabled="!enableSaveRule"
      @click="store.saveRule"
    >
      Save Rule
    </FeatherButton>
    <hr />
    <ButtonWithSpinner
      :isFetching="mutations.isFetching.value"
      class="save-btn"
      primary
      :disabled="!store.selectedPolicy.rules.length || !store.selectedPolicy.name"
      @click="store.savePolicy"
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
const enableSaveRule = computed(() => store.selectedRule?.name && store.selectedRule?.triggerEvents.length)
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
