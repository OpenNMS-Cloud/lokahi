<template>
  <div class="line-chart-container">
    <div class="chart-container">
      <Line
        :data="chartData"
        :options="chartOptions"
        ref="lineChart"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import useTheme from '@/composables/useTheme'
import { ChartData } from '@/types'
import { PropType } from 'vue'
import { Line } from 'vue-chartjs'
import { downloadCanvas } from '../Graphs/utils'
import 'chartjs-adapter-date-fns'

import {
  Chart,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  ChartOptions
} from 'chart.js'
import { humanFileSizeFromBits, getColorFromFeatherVar, getChartGridColor } from '../utils'
const { onThemeChange, isDark } = useTheme()

Chart.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend)

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
  },
  format: {
    required: true,
    type: Function as PropType<(val: string) => string>
  },
  getChartAreaWidthForDataPoints: {
    required: true,
    type: Function as PropType<(width: number) => void>
  },
  labelSuffix: {
    required: false,
    type: String,
    default: ''
  }
})
const lineChart = ref()

const downloadChart = (filename: string) => {
  if (lineChart.value) {
    downloadCanvas(lineChart.value.chart.canvas, `${filename + Date.now()}`)
  }
}

const chartOptions = computed<ChartOptions<any>>(() => {
  return {
    indexAxis: 'x',
    responsive: true,
    cubicInterpolationMode: 'monotone',
    maintainAspectRatio: false,
    interaction: {
      mode: 'nearest',
      intersect: false,
      axis: 'x'
    },
    animation: {
      duration: 700
    },
    plugins: {
      legend: {
        display: true,
        position: 'right',
        labels: {
          boxWidth: 13,
          boxHeight: 13,
          useBorderRadius: true,
          padding: 16,
          borderRadius: 1,
          color: colorFromFeatherVar.value,
          font: {
            weight: 400
          }
        },
        onHover: (event: any, activeElements: any) => {
          ;(event?.native?.target as HTMLElement).style.cursor = activeElements?.length > 0 ? 'pointer' : 'auto'
        }
      },

      tooltip: {
        xAlign: 'left',
        yAlign: 'bottom',
        position: 'nearest',
        backgroundColor: 'rgba(255, 255, 255, 1)',
        borderColor: 'rgba(0, 0, 0, )',
        borderWidth: 0.1,
        bodyColor: '#4F4F4F',
        footerColor: '#4F4F4F',
        titleColor: '#4F4F4F',
        callbacks: {
          title: (context: any) => context.label,
          label: (context: any) => {
            const appName = context.dataset.label
            return `${appName} : ` + humanFileSizeFromBits(context.parsed.y) + props.labelSuffix
          }
        }
      }
    },
    scales: {
      x: {
        type: 'time',
        stacked: true,
        grid: {
          display: true,
          color: getChartGridColor(isDark.value)
        },
        ticks: {
          callback: (val: number) => props.format(new Date(val).toISOString()),
          color: colorFromFeatherVar.value
        }
      },
      y: {
        grid: {
          display: false
        },
        ticks: {
          callback: function (value: any) {
            return humanFileSizeFromBits(value) + props.labelSuffix
          },
          color: colorFromFeatherVar.value
        },
        title: {
          display: true,
          align: 'center'
        }
      }
    }
  }
})

const colorFromFeatherVar = computed(() =>
  isDark.value ? getColorFromFeatherVar('primary-text-on-color') : getColorFromFeatherVar('primary-text-on-surface')
)

onThemeChange(() => {
  chartOptions.value.scales.x.grid.color = getChartGridColor(isDark.value)
})

const getChartAreaWidth = () => {
  if (lineChart.value?.chart) {
    props.getChartAreaWidthForDataPoints(lineChart.value.chart.chartArea.width)
  }
}

onMounted(() => getChartAreaWidth())
useResizeObserver(lineChart, () => getChartAreaWidth())

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

.line-chart-container {
  display: flex;
  justify-content: flex-start;
  align-items: stretch;
  align-content: center;
  flex-direction: row;
  min-width: 0;
  flex-wrap: wrap;
  padding-bottom: var(variables.$spacing-m);

  .chart-container {
    flex: 1 1 0;
    min-height: 380px;
  }
}
</style>
