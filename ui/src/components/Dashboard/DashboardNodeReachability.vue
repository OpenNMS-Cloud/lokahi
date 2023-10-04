<template>
  <div class="container border">
    <span class="title">Node Reachability</span>
    <Doughnut
      :data="data"
      :options="options"
      :plugins="plugins"
      class="graph"
    />
  </div>
</template>

<script setup lang="ts">
import { Doughnut } from 'vue-chartjs'
import { useDashboardStore } from '@/store/Views/dashboardStore'
import { getColorFromFeatherVar } from '../utils'
import { ChartOptions, Chart } from 'chart.js'

const store = useDashboardStore()

const doughnutCentreText = {
  id: 'doughnutCentreText',
  beforeDatasetsDraw(chart: Chart) {
    
    const { ctx, data } = chart

    ctx.save()

    const reachable = data.datasets[0].data[0] as number
    const unreachable = data.datasets[0].data[1] as number
    const availability = Math.round(reachable / (reachable  + unreachable) * 100)
    const text = `${availability}%`

    const xCoord = chart.getDatasetMeta(0).data[0].x
    const yCoord = chart.getDatasetMeta(0).data[0].y
    ctx.font = '30px inter'
    ctx.fillStyle = getColorFromFeatherVar('shade-1') as string
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(text, xCoord, yCoord - 10)
    ctx.font = '12px inter'
    ctx.fillText('Available', xCoord, yCoord + 15)
  }
}

const plugins = [doughnutCentreText]

const data = computed(() => {
  return {
    labels: ['Responding', 'Not Responding'],
    datasets: [
      {
        data: [store.reachability.responding, store.reachability.unresponsive],
        backgroundColor: [getColorFromFeatherVar('success'), getColorFromFeatherVar('error')] as any
      }
    ]
  }
})

const options: ChartOptions<any> = {
  cutout: 75,
  plugins: {
    legend: {
      position: 'bottom',
      labels: {
        usePointStyle: true,
        padding: 30
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/vars.scss';

.container {
  border-radius: vars.$border-radius-surface;
  background: var(variables.$surface);
  padding: 20px 0px;
  height: 400px;
  width: 300px;
  margin-bottom: 20px;

  .graph {
    padding: 20px;
  }

  .title {
    display: block;
    @include typography.headline3;
    margin: 2px 0px 10px 20px;
  }
}
</style>
