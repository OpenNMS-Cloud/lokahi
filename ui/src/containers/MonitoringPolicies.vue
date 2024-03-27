<template>
  <div class="container">
    <div class="content">
      <div class="header">
        <HeadlinePage text="Monitoring Policies" />
        <FeatherButton
          primary
          @click="onCreatePolicy"
          data-test="create-policy-btn"
          >Create Policy
        </FeatherButton>
      </div>
      <div class="policies-content">
        <section class="feather-row">
          <div :class="isEditing ? 'feather-col-8' : 'feather-col-12'">
            <MonitoringPoliciesTable
              @policy-selected="isEditing = true"
            />
          </div>
          <div v-if="isEditing" class="feather-col-4">
            <MonitoringPoliciesDetailPanel
              @on-close="isEditing = false"
            />
          </div>
        </section>
     </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'

const isEditing = ref(false)
const store = useMonitoringPoliciesStore()

const onCreatePolicy = () => {
  console.log('onCreatePolicy clicked')
}

onMounted(() => store.getMonitoringPolicies())
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/lib/grid';
@use '@featherds/styles/mixins/typography';
@use '@featherds/table/scss/table';
@use '@/styles/vars.scss';

.container {
  display: flex;
  justify-content: center;
}

.content {
  width: 88%;
  margin-right: var(variables.$spacing-l);
  margin-left: var(variables.$spacing-l);
}

.header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.policies-content {
  background: var(variables.$surface);
  padding: var(variables.$spacing-l);
  border-radius: vars.$border-radius-surface;
  border: 1px solid var(variables.$border-on-surface);
}


/*
.full-page-container {
  display: flex;
  flex-direction: column;
  max-width: 2000px;
}

:deep(hr) {
  width: 100%;
  border: 1px solid var(variables.$shade-4);
  margin: var(variables.$spacing-xl) 0px;
}
*/
</style>
