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
        @item-selected="flowsStore.onDateFilterUpdate"
      />
      <div class="filters-divider"></div>
      <BasicAutocomplete
        class="filter-autocomplete"
        :get-items="getAppliications"
        :items="applicationsAutoComplete"
        label="Filter Exporters"
        ref="exportersAutocompleteRef"
      />
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
      <div class="top-of-flows">
        <div class="total-container">
          <div class="total-title">Total Flows:</div>
          <div class="total-flows">{{ flowsStore.totalFlows }}</div>
        </div>
        <div class="utilitys">
          <FeatherButton icon="Add">
            <FeatherIcon
              class="utility-icon"
              :icon="Download"
            >
            </FeatherIcon>
          </FeatherButton>
          <FeatherButton icon="Add">
            <FeatherIcon
              class="utility-icon"
              :icon="Refresh"
            >
            </FeatherIcon>
          </FeatherButton>
        </div>
      </div>

      <div class="options-container">
        <div class="data-style-container">
          <FeatherRadioGroup
            :label="'Data Style:'"
            v-model="flowsStore.filters.dataStyle.selectedItem"
            @update:model-value="(e: any) => flowsStore.filters.dataStyle.selectedItem = e"
          >
            <FeatherRadio
              v-for="item in dataStyleRadios"
              :value="item.value"
              :key="item.name"
              >{{ item.name }}</FeatherRadio
            >
          </FeatherRadioGroup>
        </div>
        <div class="traffic-container">
          <FeatherRadioGroup
            :label="'Traffic:'"
            v-model="flowsStore.filters.traffic.selectedItem"
            @update:model-value="flowsStore.trafficRadioOnChange"
          >
            <FeatherRadio
              v-for="item in trafficRadios"
              :value="item.value"
              :key="item.name"
              >{{ item.name }}</FeatherRadio
            >
          </FeatherRadioGroup>
        </div>
      </div>
      <ExpandingChartWrapper
        :title="'Top Ten Exporters (24 Hrs) - Total'"
        :model-value="flowsStore.exporters.expansionOpen"
        :on-filter-click="(e) => flowsStore.filterDialogToggle(e, false)"
      >
        <TableChart
          v-if="flowsStore.filters.dataStyle.selectedItem === 'table'"
          :id="'tableChartExporters'"
          :selected-filter-range="flowsStore.filters.dateFilter"
          :chart-data="flowsStore.exporters.tableChartData"
          :table-data="flowsStore.tableDatasets"
        />
        <LineChart
          v-if="flowsStore.filters.dataStyle.selectedItem === 'line'"
          :id="'lineChartExporters'"
          :selected-filter-range="flowsStore.filters.dateFilter"
          :chart-data="flowsStore.exporters.lineChartData"
          :table-data="flowsStore.tableDatasets"
        />
      </ExpandingChartWrapper>

      <ExpandingChartWrapper
        :title="'Top Ten Applications (24 Hrs) - Total'"
        :model-value="flowsStore.applications.expansionOpen"
        :on-filter-click="(e) => flowsStore.filterDialogToggle(e, true)"
      >
        <TableChart
          v-if="flowsStore.filters.dataStyle.selectedItem === 'table'"
          :id="'tableChartApplications'"
          :selected-filter-range="flowsStore.filters.dateFilter"
          :chart-data="flowsStore.applications.tableChartData"
          :table-data="flowsStore.tableDatasets"
        />
        <LineChart
          v-if="flowsStore.filters.dataStyle.selectedItem === 'line'"
          :id="'lineChartApplications'"
          :selected-filter-range="flowsStore.filters.dateFilter"
          :chart-data="flowsStore.applications.lineChartData"
          :table-data="flowsStore.tableDatasets"
        />
      </ExpandingChartWrapper>
    </div>
  </div>
  <FeatherDialog
    id="appDialog"
    v-model="flowsStore.applications.filterDialogOpen"
    :labels="appDialogLabels"
    @update:model-value="(e) => (flowsStore.applications.filterDialogOpen = e)"
  >
    <FeatherCheckboxGroup
      label=""
      vertical
      class="chart-dialog-group"
    >
      <FeatherCheckbox v-model="flowsStore.applications.dialogFilters.http">HTTP</FeatherCheckbox>
      <FeatherCheckbox v-model="flowsStore.applications.dialogFilters.https">HTTPS</FeatherCheckbox>
      <FeatherCheckbox v-model="flowsStore.applications.dialogFilters.pandoPub">Pando-Pub</FeatherCheckbox>
      <FeatherCheckbox v-model="flowsStore.applications.dialogFilters.snmp">SNMP</FeatherCheckbox>
      <FeatherCheckbox v-model="flowsStore.applications.dialogFilters.imaps">IMAPS</FeatherCheckbox>
    </FeatherCheckboxGroup>

    <template v-slot:footer>
      <FeatherButton
        primary
        @click="flowsStore.appDialogRefreshClick"
        >Refresh</FeatherButton
      >
    </template>
  </FeatherDialog>
  <FeatherDialog
    v-model="flowsStore.exporters.filterDialogOpen"
    :labels="expDialogLabels"
    @update:model-value="(e) => (flowsStore.exporters.filterDialogOpen = e)"
  >
    <FeatherCheckboxGroup
      label=""
      vertical
      class="chart-dialog-group"
    >
      <FeatherCheckbox v-model="flowsStore.exporters.dialogFilters.http">HTTP</FeatherCheckbox>
      <FeatherCheckbox v-model="flowsStore.exporters.dialogFilters.https">HTTPS</FeatherCheckbox>
      <FeatherCheckbox v-model="flowsStore.exporters.dialogFilters.pandoPub">Pando-Pub</FeatherCheckbox>
      <FeatherCheckbox v-model="flowsStore.exporters.dialogFilters.snmp">SNMP</FeatherCheckbox>
      <FeatherCheckbox v-model="flowsStore.exporters.dialogFilters.imaps">IMAPS</FeatherCheckbox>
    </FeatherCheckboxGroup>

    <template v-slot:footer>
      <FeatherButton
        primary
        @click="flowsStore.expDialogRefreshClick"
        >Refresh</FeatherButton
      >
    </template>
  </FeatherDialog>
</template>

<script setup lang="ts">
import { useFlowsStore } from '@/store/Views/flowsStore'
import { FeatherRadioObject } from '@/types'
import Download from '@featherds/icon/action/DownloadFile'
import Refresh from '@featherds/icon/navigation/Refresh'
const flowsStore = useFlowsStore()

const trafficRadios = ref([
  { name: 'Total', value: 'total' },
  { name: 'Inbound', value: 'inbound' },
  { name: 'Outbound', value: 'outbound' }
] as FeatherRadioObject[])

const dataStyleRadios = ref([
  { name: 'Line Chart', value: 'line' },
  { name: 'Table Chart', value: 'table' }
] as FeatherRadioObject[])

const appDialogLabels = {
  title: 'Top Ten Applications (24 Hrs) - Total'
}
const expDialogLabels = {
  title: 'Top Ten Exporters (24 Hrs) - Total'
}

onBeforeMount(async () => {
  //Get Table data first as line data will take some time to get.
  //Show Table chart first for same reason
  flowsStore.generateTableChart()
  flowsStore.generateLineChart()
})

// DUMMY DATA
const timeOptions = [
  { id: 'today', name: 'Today' },
  { id: '24h', name: 'Last 24 hours' },
  { id: '7d', name: 'Last 7 days' }
]
const applicationsAutoComplete = ref([
  { id: 'app1', name: 'Application 1' },
  { id: 'app2', name: 'Application 2' },
  { id: 'app3', name: 'Application 3' }
])
const getAppliications = () => {
  return {}
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins.scss';
@import '@featherds/styles/mixins/typography';

.flows {
  width: 100%;
  min-width: 400px;
  border: 1px solid var(variables.$border-on-surface);
  border-radius: vars.$border-radius-s;
  padding: var(variables.$spacing-m) 40px;
  background-color: var(variables.$surface);
  display: flex;
  flex-direction: column;
  gap: var(variables.$spacing-m);

  &-container {
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
  .filter-autocomplete {
    width: 100%;
    max-width: 360px;
  }
  &-divider {
    border: 1px solid #74757d;
    display: flex;
    width: 0px;
    align-self: stretch;
  }
  :deep(.feather-input-wrapper-container) {
    background-color: var(variables.$surface);
  }
}
.chart-dialog-group {
  min-width: 325px;
}

.options-container {
  display: flex;
  gap: 56px;
  margin-bottom: var(variables.$spacing-l);

  :deep(.feather-radio-group-container) {
    display: flex;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;

    label {
      margin-bottom: 0;
      margin-right: var(variables.$spacing-m);
    }

    .layout-container {
      margin-right: var(variables.$spacing-s);
    }
  }
}
.top-of-flows {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .utility-icon {
    color: rgba(0, 0, 0, 0.6);
  }
}
.total-container {
  display: flex;
  gap: var(variables.$spacing-s);
  align-items: center;

  .total-title {
    @include subtitle1();
  }
  .total-flows {
    background-color: rgba(0, 102, 109, 0.12);
    color: #00666d;
    padding: 4px 8px;
    border-radius: 4px;
  }
}
</style>
