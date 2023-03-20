<template>
  <div class="flows-container">
    <PageHeadline
    text="Flows"
    data-test="flows-page-header"
    />
    <!-- Filter Area -->
    <div class="filters">
      <BasicChipSelect
          :list="timeOptions"
          :size="160"
          :show-chip="true"
        />
      <!-- Exporters Not In Scope Yet -->

         <!-- <div class="filters-divider"></div>
        <BasicAutocomplete
        class="filter-autocomplete"
        :get-items="getExporters"
        :items="exportersAutoComplete"
        label="Filter Exporters"
        ref="exportersAutocompleteRef"
        /> -->

      <div class="filters-divider"></div>
      <BasicAutocomplete
        class="filter-autocomplete"
        :get-items="getAppliications"
        :items="applicationsAutoComplete"
        label="Filter Applications"
        ref="appsAutocompleteRef"
      />
    </div>
    <!-- Chart Area -->
    <div class="flows">
      <TableChart
      :id="'tableChart'"
      :selected-filter-range="flowsStore.dateFilter"
      :chart-data="flowsStore.tableChartData"
      :table-data="flowsStore.datasets">
      </TableChart>
    </div>
  </div>
  
</template>

<script setup lang="ts">
import { useFlowsStore } from '@/store/Views/flowsStore'

const flowsStore = useFlowsStore()

onBeforeMount(async () => {
  flowsStore.generateTableChart()
})

// DUMMY DATA
const timeOptions = [
  { id: 'today', name: 'Today' },
  { id: '24h', name: 'Last 24 hours' },
  { id: 'week', name: 'Last 7 days' }
]
const applicationsAutoComplete = ref([
  { id: 'app1', name: 'Application 1' },
  { id: 'app2', name: 'Application 2' },
  { id: 'app3', name: 'Application 3' }
])
const getAppliications = () => { return {} }

</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins.scss';
@use '@featherds/styles/mixins/typography';

.flows {
  width: 100%;
  min-width: 400px;
  border: 1px solid var(variables.$border-on-surface);
  border-radius: vars.$border-radius-s;
  padding: var(variables.$spacing-m);
  background-color: var(variables.$surface);

  &-container{
      margin: var(variables.$spacing-m);
      @include mediaQueriesMixins.screen-md {
      margin: 40px 80px;
    }
  }
  
}
.filters {
    margin-bottom: var(variables.$spacing-m);
    display: flex;
    justify-content: flex-start;
    align-items: flex-start;
    align-content: stretch;
    gap: var(variables.$spacing-xl);
    .filter-autocomplete{
      width: 100%;
      max-width: 360px;
    }
    &-divider {
      border: 1px solid #74757D;
      display: flex;
      width: 0px;
      align-self: stretch;
    }
    :deep(.feather-input-wrapper-container) {
      background-color: var(variables.$surface);
    }
}
</style>
