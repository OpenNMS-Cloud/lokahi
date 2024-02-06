<template>
  <div class="container border">
    <span class="title">Node Reachability</span>
    <Doughnut
      :data="data"
      :options="options"
      :plugins="plugins"
      class="graph"
      v-if="display"
    />
  </div>
</template>

<script setup lang="ts">
import { Doughnut } from 'vue-chartjs'
import { useDashboardStore } from '@/store/Views/dashboardStore'
import { getColorFromFeatherVar } from '../utils'
import { ChartOptions, Chart } from 'chart.js'
import useTheme from '@/composables/useTheme'

const { onThemeChange, isDark } = useTheme()
const store = useDashboardStore()
const display = ref(true)

const doughnutCentreText = {
  id: 'doughnutCentreText',
  beforeDatasetsDraw(chart: Chart) {
    const { ctx, data } = chart

    ctx.save()

    const reachable = data.datasets[0].data[0] as number
    const unreachable = data.datasets[0].data[1] as number
    const availability = Math.round((reachable / (reachable + unreachable)) * 100)
    const text = isNaN(availability) ? '0%' : `${availability}%`

    const xCoord = chart.getDatasetMeta(0).data[0].x
    const yCoord = chart.getDatasetMeta(0).data[0].y
    setTimeout(() => {
      ctx.fillStyle = isDark.value ? '#FFF' : (getColorFromFeatherVar('shade-1') as string)
      ctx.font = '30px inter'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(text, xCoord, yCoord - 10)
      ctx.font = '12px inter'
      ctx.fillText('Available', xCoord, yCoord + 15)
    })
  }
}

let plugins = [doughnutCentreText]

const data = computed(() => {
  return {
    labels: [
      `${store.reachability.responding ? `Responding (${store.reachability.responding})` : 'Responding'}`,
      `${store.reachability.unresponsive ? `Not Responding (${store.reachability.unresponsive})` : 'Not Responding'}`
    ],
    datasets: [
      {
        data: [store.reachability.responding, store.reachability.unresponsive],
        backgroundColor: [getColorFromFeatherVar('success'), getColorFromFeatherVar('error')] as any
      }
    ]
  }
})

const colorFromFeatherVar = computed(() =>
  isDark.value ? getColorFromFeatherVar('primary-text-on-color') : getColorFromFeatherVar('primary-text-on-surface')
)

const options: ChartOptions<any> = computed(() => ({
  cutout: 75,
  plugins: {
    legend: {
      position: 'bottom',
      labels: {
        usePointStyle: true,
        padding: 30,
        color: colorFromFeatherVar.value
      }
    }
  },
  animation: false
}))

onThemeChange(() => {
  display.value = false
  setTimeout(() => {
    display.value = true
  })
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/vars.scss';

.container {
  border-radius: vars.$border-radius-surface;
  background: var(variables.$surface);
  padding: 20px 0px;
  height: 442px;
  width: 350px;
  margin-bottom: 20px;

  .graph {
    padding: 10px;
  }

  .title {
    display: block;
    @include typography.headline3;
    margin: 8px 0px 10px 25px;
  }
}
</style>
