<template>
  <div class="btns">
    <FeatherButton primary :disabled="!enableSaveRule" @click="store.saveRule">
      Save Rule
    </FeatherButton>
    <FeatherButton primary :disabled="!store.selectedPolicy.rules.length" @click="store.savePolicy">
      Save Policy
    </FeatherButton>
  </div>
</template>

<script setup lang="ts">
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
const store = useMonitoringPoliciesStore()
const enableSaveRule = computed(() => {
  return store.selectedRule.name
   && store.selectedRule.componentType
   && store.selectedRule.detectionMethod
   && store.selectedRule.metricName
   && store.selectedRule.conditions.length
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
.btns {
  margin-bottom: var(variables.$spacing-xxl);
}
</style>
