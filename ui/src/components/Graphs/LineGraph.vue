<template>
  <div
    v-if="graphs.dataSets.value.length"
    class="container"
  >
    <div class="canvas-wrapper">
      <FeatherTooltip
        title="Download to PDF"
        v-slot="{ attrs, on }"
      >
        <FeatherButton
          v-bind="attrs"
          v-on="on"
          icon="Download"
          class="download-icon"
          @click="onDownload"
        >
          <FeatherIcon :icon="DownloadFile" />
        </FeatherButton>
      </FeatherTooltip>
      <canvas
        class="canvas"
        :id="`${graph.label}`"
      ></canvas>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useGraphs } from '@/composables/useGraphs'
import { ChartOptions, TitleOptions, ChartData } from 'chart.js'
import Chart from 'chart.js/auto'
// import zoomPlugin from 'chartjs-plugin-zoom'
import { PropType } from 'vue'
import { formatTimestamp, downloadCanvas } from './utils'
import { GraphProps } from '@/types/graphs'
import DownloadFile from '@featherds/icon/action/DownloadFile'
import { format } from 'd3'
import useTheme from '@/composables/useTheme'
const emits = defineEmits(['has-data'])
// Chart.register(zoomPlugin) disable zoom until phase 2
const graphs = useGraphs()
const props = defineProps({
  graph: {
    required: true,
    type: Object as PropType<GraphProps>
  }
})
const { onThemeChange, isDark } = useTheme()
const yAxisFormatter = format('.3s')
let chart: any = {}
const options = computed<ChartOptions<any>>(() => ({
  responsive: true,
  aspectRatio: 1.4,
  plugins: {
    title: {
      display: true,
      text: props.graph.label
    } as TitleOptions,
    zoom: {
      zoom: {
        wheel: {
          enabled: true
        },
        mode: 'x'
      },
      pan: {
        enabled: true,
        mode: 'x'
      }
    }
  },
  scales: {
    y: {
      title: {
        display: false,
        text: props.graph.label
      } as TitleOptions,
      ticks: {
        callback: (value: any, index: any) => (index % 2 === 0 ? yAxisFormatter(value as number) : ''),
        maxTicksLimit: 8
      },
      stacked: false
    },
    x: {
      ticks: {
        maxTicksLimit: 12
      },
      grid: {
        display: true,
        color: isDark.value ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)'
      }
    }
  }
}))
const xAxisLabels = computed(() => {
  const graphsDataSetsValues = (graphs.dataSets.value[0]?.values as any) || ([] as any)
  const totalLabels = graphsDataSetsValues.map((val: any) => {
    return formatTimestamp(val[0], 'minutes')
  })
  return totalLabels
})
const dataSets = computed(() => {
  const bgColor = ['green', 'blue'] // TODO: find solution to set bg color, in regards to FeatherDS theme switching
  emits('has-data', graphs.dataSets.value.length)
  return graphs.dataSets.value.map((data: any, i) => ({
    label: props.graph.metrics[i],
    data: data.values.map((val: any) => val[1]),
    backgroundColor: bgColor[i],
    borderColor: bgColor[i],
    hitRadius: 5,
    hoverRadius: 6
  }))
})
const chartData = computed<ChartData<any>>(() => {
  return {
    labels: xAxisLabels.value,
    datasets: dataSets.value
  }
})
const render = async (update?: boolean) => {
  try {
    if (update) {
      chart.data = chartData.value
      chart.update()
    } else {
      if (chartData.value.datasets.length) {
        const ctx: any = document.getElementById(`${props.graph.label}`)
        chart = new Chart(ctx, {
          type: 'line',
          data: chartData.value,
          options: options.value,
          plugins: []
        })
      }
    }
  } catch (error) {
    console.log(error)
    console.log('Could not render graph for ', props.graph.label)
  }
}
const onDownload = () => {
  const canvas = document.getElementById(props.graph.label) as HTMLCanvasElement
  downloadCanvas(canvas, props.graph.label)
}
onMounted(async () => {
  await graphs.getMetrics(props.graph)
  render()
})
onThemeChange(() => {
  options.value.scales.x.grid.color = isDark.value ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)'
})
</script>

<!-- TODO: make theme switching works in graphs -->
<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
.container {
  position: relative;
  width: 30%;
  min-width: 380px;
  border: 1px solid var(variables.$secondary-text-on-surface);
  border-radius: 10px;
  padding: var(variables.$spacing-m);
}
.canvas-wrapper {
  width: 100%;
  .download-icon {
    position: absolute;
    right: 15px;
    top: 19px;
    svg {
      font-size: 15px;
    }
  }
}
</style>
