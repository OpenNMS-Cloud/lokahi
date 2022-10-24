<template>
  <div class="container">
      <div class="canvas-wrapper">
        <canvas :id="`${label}`"></canvas>
      </div>
  </div>
</template>
  
<script setup lang="ts">
import { useGraphs } from '@/composables/useGraphs'
import { ChartOptions, TitleOptions, ChartData } from 'chart.js'
import { Chart, registerables }  from 'chart.js'
// import zoomPlugin from 'chartjs-plugin-zoom'
import { PropType } from 'vue'
import { formatTimestamp } from './utils'
import { uniq } from 'lodash'
Chart.register(...registerables)
// Chart.register(zoomPlugin) disable zoom until phase 2

const graphs = useGraphs()

const props = defineProps({
  metricStrings: {
    required: true,
    type: Array as PropType<string[]>
  },
  label: {
    required: true,
    type: String
  }
})

let chart: any = {}

const options = computed<ChartOptions>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    title: {
      display: true,
      text: props.label,
    } as TitleOptions,
    zoom: {
      zoom: {
        wheel: {
          enabled: true,
        },
        mode: 'x',
      },
      pan: {
        enabled: true,
        mode: 'x'
      }
    },
  },
  scales: {
    y: {
      title: {
        display: true,
        text: props.label,
      } as TitleOptions,
      ticks: {
        maxTicksLimit: 8
      },
      stacked: false
    },
    x: {
      ticks: {
        maxTicksLimit: 12
      }
    }
  }
}))

const xAxisLabels = computed(() => {
  const totalLabels = []

  for (const dataSet of graphs.dataSets.value) {
    const labels = dataSet.map((result) => {
      if (result.value) return formatTimestamp(result.value[0], 'hours')
    })
    totalLabels.push(...labels)
  }

  return uniq(totalLabels)
})

const dataSets = computed(() => {
  const dataSets: any = []

  for (const dataSet of graphs.dataSets.value) {
    const dataObject = {
      label: dataSet[0].metric.__name__,
      data: dataSet.map((result) => {
        if (result.value) return result.value[1]
      }),
      backgroundColor: 'green'
    }

    dataSets.push(dataObject)
  }
  
  return dataSets
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
      const ctx: any = document.getElementById(`${props.label}`)
      chart = new Chart(ctx, {
        type: 'line',
        data: chartData.value,
        options: options.value,
        plugins: []
      })
    }
  } catch (error) {
    console.log(error)
    console.log('Could not render graph for ', props.label)
  }
}

onMounted(async () => {
  await graphs.getMetrics(props.metricStrings)
  render()
})
</script>
  
<style scoped lang="scss">
@import "@featherds/styles/mixins/typography";
.container {
  position: relative;
}
.canvas-wrapper {
  display: block;
  height: 300px;
}
</style>
