<template>
  <DashboardEmptyState
    :texts="dashboardText.NetworkTraffic"
    v-if="networkTrafficIn.length <= 1 && networkTrafficOut.length <= 1"
  >
    <template v-slot:icon>
      <FeatherIcon :icon="isDark ? AreaChartDark : AreaChart" class="empty-chart-icon" />
    </template>
  </DashboardEmptyState>
  <Line v-else :data="dataGraph" :options="configGraph" />
</template>

<script setup lang="ts">
import dashboardText from '@/components/Dashboard/dashboard.text'
import AreaChart from '@/assets/AreaChart.svg'
import AreaChartDark from '@/assets/AreaChart-dark.svg'
import useTheme from '@/composables/useTheme'
import { Line } from 'vue-chartjs'
import { format, fromUnixTime } from 'date-fns'
import { useDashboardStore } from '@/store/Views/dashboardStore'
import { optionsGraph } from './dashboardNetworkTraffic.config'
import { ChartData } from '@/types'
import { ChartOptions } from 'chart.js'
import 'chartjs-adapter-date-fns'
import { humanFileSize } from '../utils'

const { onThemeChange, isDark } = useTheme()

const store = useDashboardStore()
const networkTrafficIn = ref([] as [number, number][])
const networkTrafficOut = ref([] as [number, number][])
const dataGraph = ref({} as ChartData)
const configGraph = ref({})


onMounted(async () => {
  await store.getNetworkTrafficInValues()
  await store.getNetworkTrafficOutValues()
})

//format data for the graph
const formatValues = (list: [number, number][]): [number, number][] =>
  list.map((i) => {
    const transformTimeStampToMillis = (val: number) => val * 1000
    const transformtoGb = (val: number) => val
    return [transformTimeStampToMillis(i[0]), transformtoGb(i[1])]
  })

const createConfigGraph = (list: number[]) => {
  const options: Required<ChartOptions> = { ...optionsGraph }
  options.aspectRatio = 1.4
  options.scales.y = {
    ticks: {
      callback: (value) => humanFileSize(Number(value)),
      maxTicksLimit: 8
    },
    position: 'right'
  }
  options.scales.x = {
    type: 'time',
    grid: {
      display: false
    },
    ticks: {
      callback(val, index): string {
        //show date at the beginning and at the end
        const lastIndex = store.totalNetworkTrafficIn.length - 1

        if ((index === 0 || index === lastIndex) && list[index]) {
          return format(fromUnixTime(list[index]), 'LLL d')
        }

        return index % 2 === 0 ? format(new Date(val), 'kk:mm') : ''
      },
      maxTicksLimit: 12
    },
    time: {
      tooltipFormat: 'kk:mm'
    }
  }
  return options
}

const createData = (list: [number, number][]) => {
  return {
    labels: list.map((i) => i[0]),
    datasets: [
      {
        borderWidth: 0,
        data: networkTrafficIn.value,
        label: 'Outbound',
        spanGaps: 1000 * 60 * 3 // no line over 3+ minute gaps
      },
      {
        borderWidth: 0,
        data: networkTrafficOut.value,
        label: 'Inbound',
        spanGaps: 1000 * 60 * 3
      }
    ]
  }
}

watchEffect(() => {
  // add a final point 'now', which is used to track the gap between the previous point and now
  const totalTrafficIn = [...store.totalNetworkTrafficIn]
  totalTrafficIn.push([Date.now() / 1000, 0])

  const totalTrafficOut = [...store.totalNetworkTrafficOut]
  totalTrafficOut.push([Date.now() / 1000, 0])

  networkTrafficIn.value = formatValues(totalTrafficIn)
  networkTrafficOut.value = formatValues(totalTrafficOut)
  dataGraph.value = createData(networkTrafficIn.value)
  const dates = store.totalNetworkTrafficIn.map((v) => v[0]) //dates before format
  configGraph.value = createConfigGraph(dates)
})

onThemeChange(() => {
  optionsGraph.plugins.legend.labels.color = isDark.value ? '#d1d0d0' : '#00000'
  optionsGraph.scales.x.ticks.color = isDark.value ? '#d1d0d0' : '#00000'
  optionsGraph.scales.y.ticks.color = isDark.value ? '#d1d0d0' : '#00000'
  configGraph.value = { ...optionsGraph }
})
</script>
