<template>
  <div class="container">
    <HeadlinePage text="Monitoring Policies" />
    <MonitoringPoliciesPolicyForm />
    <MonitoringPoliciesRuleForm />
    <MonitoringPoliciesSaveButtons />
  </div>
</template>

<script setup lang="ts">
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
const store = useMonitoringPoliciesStore()
onMounted(() => store.getMonitoringPolicies())
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.container {
  display: flex;
  margin: 0 2rem;
  flex-direction: column;
  max-width: 2000px;
}

:deep(hr) {
  width: 100%;
  border: 1px solid var(variables.$shade-4);
  margin: var(variables.$spacing-xl) 0px;
}
</style>
