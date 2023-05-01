<template>
  <DashboardEmptyState
    :texts="dashboardText.NetworkTraffic"
    v-if="networkTrafficIn.length < 1 && networkTrafficOut.length < 1"
  >
    <template v-slot:icon>
      <FeatherIcon
        :icon="isDark ? AreaChartDark : AreaChart"
        class="empty-chart-icon"
      />
    </template>
  </DashboardEmptyState>
  <Line
    v-else
    :data="dataGraph"
    :options="configGraph"
  />
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

const { onThemeChange, isDark } = useTheme()

const store = useDashboardStore()
const networkTrafficIn = ref([] as [string, number][])
const networkTrafficOut = ref([] as [string, number][])
const dataGraph = ref({} as ChartData)
const configGraph = ref({})

onMounted(async () => {
  await store.getNetworkTrafficInValues()
  await store.getNetworkTrafficOutValues()
})

//format data for the graph
const formatValues = (list: [number, number][]): [string, number][] =>
  list.map((i) => {
    const transformToDate = (val: number) => format(fromUnixTime(val), 'h a')
    const transformtoGb = (val: number) => val / 1e9
    return [transformToDate(i[0]), transformtoGb(i[1])]
  })

const createConfigGraph = (list: number[]) => {
  const options = { ...optionsGraph }
  options.aspectRatio = 1.4
  options.scales.x = {
    grid: {
      display: false
    },
    ticks: {
      callback(val: number, index: number): string {
        //show date at the beginning and at the end
        const lastIndex = store.totalNetworkTrafficIn.length - 1

        if ((index === 0 || index === lastIndex) && list[index]) {
          const date = format(fromUnixTime(list[index]), 'LLL d')
          return (this as any).getLabelForValue(date)
        }
        return index % 2 === 0 ? (this as any).getLabelForValue(val) : ''
      }
    }
  }
  return options
}

const createData = (list: [string, number][]) => {
  return {
    labels: list.map((i) => i[0]),
    datasets: [
      {
        data: networkTrafficIn.value,
        segment: {
          borderColor: 'transparent'
        },
        label: 'Outbound'
      },
      {
        data: networkTrafficOut.value,
        segment: {
          borderColor: 'transparent'
        },
        label: 'Inbound'
      }
    ]
  }
}

watchEffect(() => {
  networkTrafficIn.value = formatValues(store.totalNetworkTrafficIn)
  networkTrafficOut.value = formatValues(store.totalNetworkTrafficOut)
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
