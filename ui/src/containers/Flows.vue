<template>
  <div class="flows-container">
    <HeadlinePage
      text="Flows"
      data-test="flows-page-header"
    />
    <!-- Filter Area -->
    <div class="filters">
      <TextRadioButtons
        :items="timeOptions"
        @checked="flowsStore.onDateFilterUpdate"
        selected-value="TODAY"
        id="Date"
      >
      </TextRadioButtons>
      <div class="filters-divider"></div>
      <div class="exporter-section">
        <FeatherAutocomplete
          class="filter-autocomplete"
          label="Filter Exporters"
          type="multi"
          v-model="flowsStore.filters.selectedExporters"
          :loading="flowsStore.filters.isExportersLoading"
          :results="flowsStore.filters.filteredExporters"
          @search="flowsStore.exportersAutoCompleteSearch"
          @update:model-value="flowsStore.updateChartData"
        ></FeatherAutocomplete>
        <FeatherButton
          @click="toggleDrawer"
          icon="Help"
        >
          <FeatherIcon
            class="utility-icon"
            :icon="HelpIcon"
          >
          </FeatherIcon>
        </FeatherButton>
      </div>

      <div class="filters-divider"></div>
      <FeatherAutocomplete
        class="filter-autocomplete"
        label="Filter Applications"
        type="multi"
        v-model="flowsStore.filters.selectedApplications"
        :loading="flowsStore.filters.isApplicationsLoading"
        :results="flowsStore.filters.filteredApplications"
        :selectionLimit="10"
        @search="flowsStore.applicationsAutoCompleteSearch"
        @update:model-value="flowsStore.updateChartData"
      ></FeatherAutocomplete>
    </div>
    <!-- Chart Area -->
    <div class="flows filters">
      <div class="top-of-flows">
        <div class="total-container">
          <div class="total-title">Total Flows:</div>
          <div class="total-flows">{{ appStore.totalFlows }}</div>
        </div>
        <div class="utilitys">
          <FeatherButton
            icon="Download"
            @click="
              flowsStore.filters.dataStyle.selectedItem === 'table'
                ? downloadTableChartApplications('TableChartApplications')
                : downloadLineChartApplications('LineChartApplications')
            "
            :disabled="
              (!appStore.hasLineData && flowsStore.filters.dataStyle.selectedItem === 'line') ||
              (!appStore.hasTableData && flowsStore.filters.dataStyle.selectedItem === 'table')
            "
          >
            <FeatherIcon
              class="utility-icon"
              :icon="Download"
            >
            </FeatherIcon>
          </FeatherButton>
          <FeatherButton
            @click="flowsStore.populateData"
            icon="Refresh"
          >
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
    </div>
    <div class="flows applications-charts">
      <div class="flows-titles">
        <div class="title">Top Ten Applications</div>
        <div class="optional-text">Optional Explainer Text</div>
      </div>
      <TableChart
        v-if="
          flowsStore.filters.dataStyle.selectedItem === 'table' && appStore.hasTableData && !appStore.isTableLoading
        "
        :id="'tableChartApplications'"
        ref="tableChartApplications"
        :selected-filter-range="flowsStore.filters.dateFilter"
        :chart-data="appStore.tableChartData"
        :table-data="appStore.tableData"
      />
      <LineChart
        v-if="flowsStore.filters.dataStyle.selectedItem === 'line' && appStore.hasLineData && !appStore.isLineLoading"
        :id="'lineChartApplications'"
        ref="lineChartApplications"
        :selected-filter-range="flowsStore.filters.dateFilter"
        :chart-data="appStore.lineChartData"
        :table-data="appStore.tableData"
      />
      <div
        v-if="
          ((!appStore.hasLineData && flowsStore.filters.dataStyle.selectedItem === 'line') ||
            (!appStore.hasTableData && flowsStore.filters.dataStyle.selectedItem === 'table')) &&
          !appStore.isLineLoading &&
          !appStore.isTableLoading
        "
      >
        <DashboardEmptyState :texts="ApplicationsText.Applications">
          <template v-slot:icon>
            <FeatherIcon
              :icon="isDark ? PolarChartDark : PolarChart"
              class="empty-chart-icon"
            />
          </template>
        </DashboardEmptyState>
      </div>
      <div v-if="appStore.isLineLoading || appStore.isTableLoading">
        <FeatherSpinner />
      </div>
    </div>
  </div>
  <FeatherDrawer
    :modelValue="isDrawerOpen"
    @update:modelValue="toggleDrawer"
    :labels="{ close: 'close', title: 'Flows' }"
  >
    <!-- This will be removed in the next iteration of design -->
    <div class="exporter-drawer">
      <h2>Flows</h2>
      <p>Flows are summaries of network traffic sent by network devices (switches, routers, and so on).</p>
      <br />
      <p>
        By default, the Flows page displays graphs showing the top ten exporters, applications, and conversations for
        the network devices that you are monitoring. You can filter on the following attributes to customize the
        information displayed:
      </p>
      <br />
      <ul>
        <li>Time period (current calendar day, last 24 hours, last 7 days)</li>
        <li>Exporters (devices configured to export flow reports)</li>
        <li>Applications (monitored protocols)</li>
      </ul>
    </div>
  </FeatherDrawer>
</template>

<script setup lang="ts">
import { useFlowsStore } from '@/store/Views/flowsStore'
import { FeatherRadioObject } from '@/types'
import { TimeRange } from '@/types/graphql'
import Download from '@featherds/icon/action/DownloadFile'
import HelpIcon from '@featherds/icon/action/Help'
import Refresh from '@featherds/icon/navigation/Refresh'
import { FeatherDrawer } from '@featherds/drawer'
import ApplicationsText from '@/components/Flows/flows.text'
import { useFlowsApplicationStore } from '@/store/Views/flowsApplicationStore'
import useTheme from '@/composables/useTheme'
import PolarChart from '@/assets/PolarChart.svg'
import PolarChartDark from '@/assets/PolarChart-dark.svg'
const flowsStore = useFlowsStore()
const appStore = useFlowsApplicationStore()
const { isDark } = useTheme()

const lineChartApplications = ref()
const downloadLineChartApplications = (fileName: string) => {
  lineChartApplications.value.downloadChart(fileName)
}

const tableChartApplications = ref()
const downloadTableChartApplications = (fileName: string) => {
  tableChartApplications.value.downloadChart(fileName)
}

const trafficRadios = ref([
  { name: 'Total', value: 'total' },
  { name: 'Inbound', value: 'inbound' },
  { name: 'Outbound', value: 'outbound' }
] as FeatherRadioObject[])

const dataStyleRadios = ref([
  { name: 'Table Chart', value: 'table' },
  { name: 'Line Chart', value: 'line' }
] as FeatherRadioObject[])

onMounted(async () => {
  await flowsStore.populateData()
})

const timeOptions = ref([
  { value: TimeRange.Today, name: 'Today' },
  { value: TimeRange.Last_24Hours, name: '24H' },
  { value: TimeRange.SevenDays, name: '7D' }
])

const isDrawerOpen = ref(false)
const toggleDrawer = () => {
  isDrawerOpen.value = !isDrawerOpen.value
}

onUnmounted(() => flowsStore.$reset)
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
.flows-titles {
  margin-bottom: var(variables.$spacing-xl);
  .title {
    @include typography.headline3;
  }
  .optional-text {
    @include typography.caption;
  }
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
  width: 100%;

  .utility-icon {
    color: rgba(0, 0, 0, 0.6);
  }
}

.open-dark {
  .top-of-flows {
    .utility-icon {
      color: rgba(255, 255, 255, 0.6);
    }
  }
}
.exporter-section {
  display: flex;
  flex-direction: row;
  gap: 8px;
  width: 100%;
  max-width: 360px;
}
.total-container {
  display: flex;
  gap: var(variables.$spacing-s);
  align-items: center;

  .total-title {
    @include typography.subtitle1;
  }
  .total-flows {
    background-color: rgba(0, 102, 109, 0.12);
    color: #00666d;
    padding: 4px 8px;
    border-radius: 4px;
  }
}
.exporter-drawer {
  max-width: 531px;
  margin: var(variables.$spacing-xl) var(variables.$spacing-l);

  li {
    list-style-type: disc;
    margin-left: var(variables.$spacing-l);
  }
}
</style>
