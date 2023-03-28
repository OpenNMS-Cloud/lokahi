<template>
  <div class="table-chart-container">
    <div class="chart-container">
      <BasicChart
        :id="id"
        :chart-options="chartOptions"
        :chart-data="chartData"
        :chart-type="ChartTypes.LINE"
      >
      </BasicChart>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChartOptions, ChartData } from 'chart.js'
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
  tableData: {
    required: true,
    type: Object
  },
  selectedFilterRange: {
    required: true,
    type: String
  }
})

const chartOptions = computed<ChartOptions<any>>(() => {
  return {
    indexAxis: 'x',
    responsive: true,
    cubicInterpolationMode: 'monotone',
    maintainAspectRatio: false,
    interaction: {
      mode: 'x'
    },
    plugins: {
      legend: {
        display: false
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
        titleColor: '#4F4F4F'
      }
    },
    scales: {
      x: {
        stacked: true,
        grid: {
          display: true
        },
        ticks: {}
      },
      y: {
        stacked: true,
        grid: {
          display: false
        },
        title: {
          display: true,
          align: 'center'
        }
      }
    }
  }
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
  .chart-container {
    flex: 1 1 0;
    min-height: 280px;
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
