<template>
  <FeatherAutocomplete
    v-if="hasData"
    class="exporters-filter"
    :label="dashboardText.TopApplications.filterLabel"
    type="single"
    v-model="flowsStore.filters.selectedExporterTopApplication"
    :loading="flowsStore.filters.isExportersLoading"
    :results="flowsStore.filters.filteredExporters"
    @search="flowsStore.exportersAutoCompleteSearch"
    @update:model-value="flowsAppStore.getApplicationDataset"
  ></FeatherAutocomplete>
  <div class="flows">
    <div
      v-if="hasData"
      class="chart-box"
    >
      <PolarArea
        :data="dataGraph"
        :options="constGraph"
        :id="'pieChartApplications'"
      />
    </div>
    <div v-else>
      <DashboardEmptyState :texts="dashboardText.TopApplications">
        <template v-slot:icon>
          <FeatherIcon
            :icon="isDark ? PolarChartDark : PolarChart"
            class="empty-chart-icon"
          />
        </template>
      </DashboardEmptyState>
    </div>
  </div>
</template>

<script setup lang="ts">
import { map, sum, sortBy } from 'lodash'
import { useFlowsStore } from '@/store/Views/flowsStore'
import useTheme from '@/composables/useTheme'
import { PolarArea } from 'vue-chartjs'
import dashboardText from '@/components/Dashboard/dashboard.text'
import PolarChart from '@/assets/PolarChart.svg'
import PolarChartDark from '@/assets/PolarChart-dark.svg'
import { useFlowsApplicationStore } from '@/store/Views/flowsApplicationStore'

const { onThemeChange, isDark } = useTheme()
const flowsStore = useFlowsStore()
const flowsAppStore = useFlowsApplicationStore()
const constGraph = ref()
const dataGraph = ref()
const hasData = ref(false)

const dataApplications = computed(() => flowsStore.topApplications)

const buildData = () => {
  if (dataApplications.value.length > 0) {
    const dataWithValues = dataApplications.value.filter((v) => v.bytesIn > 0)
    const values = map(sortBy(dataWithValues, ['bytesIn']).reverse(), 'bytesIn')
    const total = sum(values)
    const percentages = map(values, (i) => (i * 100) / total)
    const labels = map(dataWithValues, (app, i) => ` ${i + 1}. ${app.label} (${percentages[i].toFixed(2)}%)`)
    const data = {
      labels: labels,
      datasets: [
        {
          data: percentages
        }
      ]
    }
    hasData.value = true
    dataGraph.value = data
  } else {
    hasData.value = false
  }
}

const config = {
  responsive: true,
  aspectRatio: 1.5,
  plugins: {
    legend: {
      display: true,
      align: 'center',
      position: 'right',
      labels: {
        boxWidth: 22,
        boxHeight: 22,
        borderRadius: 20,
        useBorderRadius: true,
        padding: 20,
        color: isDark.value ? '#d1d0d0' : '#00000',
        font: {
          size: 15
        },
        usePointStyle: true
      }
    }
  }
}
constGraph.value = config

watchEffect(() => {
  buildData()
})

onThemeChange(() => {
  config.plugins.legend.labels.color = isDark.value ? '#d1d0d0' : '#00000'
  constGraph.value = { ...config }
})

onMounted(async () => {
  flowsStore.filters.selectedExporterTopApplication = undefined
})

onUnmounted(() => flowsStore.$reset)
</script>

<style scoped lang="scss">
@use '@/styles/mediaQueriesMixins.scss';

.exporters-filter {
  width: 100%;
  @include mediaQueriesMixins.screen-md {
    width: 50%;
    margin-left: auto;
  }
}
</style>
