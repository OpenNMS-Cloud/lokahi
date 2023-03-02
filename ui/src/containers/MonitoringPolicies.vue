<template>
  <div class="container">
    <PageHeadline text="Monitoring Policies" />
    <MPolicyForm />
    <MPolicyRuleForm/>

    <SaveButtons />
  </div>
</template>

<script setup lang="ts">
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
const store = useMonitoringPoliciesStore()
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.container {
  display: flex;
  margin: 0 2rem;
  flex-direction: column;
}
</style>
