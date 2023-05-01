<template>
  <div class="table-chart-container">
    <div class="chart-container">
      <Bar
        :data="chartData"
        :options="chartOptions"
        ref="barChart"
      />
    </div>

    <div class="table-container">
      <table class="condensed">
        <thead>
          <tr>
            <th scope="col">Total</th>
            <th scope="col">Inbound</th>
            <th scope="col">Outbound</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(data, index) in tableData"
            :key="index"
          >
            <td>{{ formatBytes(addValues(data.bytesIn, data.bytesOut)) }}</td>
            <td>{{ formatBytes(data.bytesIn) }}</td>
            <td>{{ formatBytes(data.bytesOut) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { ChartData } from '@/types'
import { Bar } from 'vue-chartjs'
import { Chart, Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, ChartOptions } from 'chart.js'
import { downloadCanvas } from '../Graphs/utils'

Chart.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend)

const props = defineProps({
  id: {
    required: true,
    type: String
  },
  chartData: {
    required: true,
    type: Object as PropType<ChartData>
  },
  tableData: {
    required: true,
    type: Object
  },
  selectedFilterRange: {
    required: true,
    type: String
  }
})

const barChart = ref()

const downloadChart = (filename: string) => {
  if (barChart.value) {
    downloadCanvas(barChart.value.chart.canvas, `${filename + Date.now()}`)
  }
}

const chartOptions = computed<ChartOptions<any>>(() => {
  return {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
        align: 'start',
        position: 'top',
        labels: {
          boxWidth: 13,
          boxHeight: 13,
          usePointStyle: true,
          useBorderRadius: true,
          borderRadius: 8,
          font: {
            weight: 700
          }
        },
        onClick: (e: Event) => e.stopPropagation()
      },
      tooltip: {
        xAlign: 'left',
        yAlign: 'bottom',
        position: 'nearest',
        backgroundColor: 'rgba(255, 255, 255, 0.78)',
        borderColor: 'rgba(0, 0, 0, )',
        borderWidth: 0.1,
        bodyColor: '#4F4F4F',
        footerColor: '#4F4F4F',
        titleColor: '#4F4F4F',
        callbacks: {
          title: () => props.selectedFilterRange,
          label: (context: any) => {
            const value = context.dataset.data[context.dataIndex]
            const labelAbbrev = context.dataset.label.substring(0, 3).toLowerCase()
            const appName = context.label
            return `${appName}(${labelAbbrev}): ` + formatBytes(value)
          }
        }
      }
    },
    scales: {
      x: {
        stacked: true,
        grid: {
          display: true
        },
        ticks: {
          callback: function (value: any) {
            return formatBytes(value, 2)
          }
        }
      },
      y: {
        stacked: true,
        grid: {
          display: false
        },
        title: {
          display: true,
          align: 'center',
          text: 'Applications'
        }
      }
    }
  }
})

const addValues = (a: number, b: number) => {
  const total = (a + b).toString()
  return parseFloat(total).toPrecision(3)
}

const formatBytes = (bytes: any, decimals = 2) => {
  if (!+bytes) return '0 Bytes'

  const k = 1024
  const dm = decimals < 0 ? 0 : decimals
  const sizes = ['Bytes', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB']

  const i = Math.floor(Math.log(bytes) / Math.log(k))
  if (sizes[i] === undefined) {
    return 0
  }
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`
}

defineExpose({
  downloadChart
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins.scss';
@use '@featherds/styles/mixins/typography';
@import '@featherds/table/scss/table';

.table-chart-container {
  display: flex;
  justify-content: flex-start;
  align-items: stretch;
  align-content: center;
  flex-direction: row;
  min-width: 0;
  flex-wrap: wrap;
  gap: var(variables.$spacing-l);
  padding-bottom: var(variables.$spacing-m);

  .table-container {
    flex: 1 1 0;
    max-width: 360px;
  }
  .chart-container {
    flex: 1 1 0;
  }
}
table {
  @include table();
  @include row-select();
  width: 100%;
  &.condensed {
    @include table-condensed();
  }
  th,
  td {
    border-bottom: 0px;
    box-shadow: none;
  }
}
</style>
