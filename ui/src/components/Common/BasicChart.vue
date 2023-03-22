<!-- 
Generic chart using ChartJS.   
  - Props
    - id: id used for canvas ID and used for error handling
    - chartData: ChartData object from chartjs.
    - chartOptions: ChartOptions object from chartjs
    - chartType: ChartType enum to select the type of chart you want to use.
  
  TODO:
    - Allow user to render the chart when they want on the parent component/store.
 -->
<template>
  <canvas
    class="canvas"
    :id="id"
  ></canvas>
</template>

<script setup lang="ts">
import { ChartOptions, ChartData, ChartType } from 'chart.js'
import Chart from 'chart.js/auto'
import { PropType } from 'vue'
import { ChartTypes } from '@/types'

const props = defineProps({
  id: {
    required: true,
    type: String
  },
  chartData: {
    required: true,
    type: Object as PropType<ChartData>
  },
  chartOptions: {
    required: true,
    type: Object as PropType<ChartOptions>
  },
  chartType: {
    required: true,
    type: String as PropType<ChartTypes>,
    default: ChartTypes.BAR
  }
})

let chart: any = {}

const render = async (update?: boolean) => {
  try {
    if (update) {
      chart.data = props.chartData
      chart.update()
    } else {
      if (props.chartData.datasets.length) {
        const ctx: any = document.getElementById(props.id)
        chart = new Chart(ctx, {
          type: props.chartType as ChartType,
          data: props.chartData,
          options: props.chartOptions
        })
      }
    }
  } catch (error) {
    console.log(error)
    console.log('Could not render chart for ', props.id)
  }
}

watch(props, () => {
  render(true)
})

onMounted(async () => {
  render()
})
</script>
