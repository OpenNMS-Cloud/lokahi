<template>
  <div class="table-chart-container">
    <div class="chart-container">
      <BasicChart
      :id="id"
      :chart-options="chartOptions"
      :chart-data="chartData"
      :chart-type="ChartTypes.BAR">
      </BasicChart>
    </div>
    
    <div class="table-container">
      <table
        class="condensed"
      >
        <thead>
          <tr>
            <th scope="col">Total</th>
            <th scope="col">Inbound</th>
            <th scope="col">Outbound</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(data, index) in tableData" :key="index">       
            <td>{{ postFixGB(addValues(data.inbound, data.outbound)) }}</td>
            <td>{{ postFixGB(data.inbound) }}</td>
            <td>{{ postFixGB(data.outbound) }}</td>
          </tr>
        </tbody>
      </table>
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
          color: 'black',
          font: {
            weight: 700
          }
        }
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
          label: (context:any) => {
            const value = context.dataset.data[context.dataIndex]
            const labelAbbrev = context.dataset.label.substring(0, 3).toLowerCase()
            const appName = context.label
            return `${appName}(${labelAbbrev}): ${value} GB`
          }
          
        }

      }
    },
    scales: {
      x: {
        stacked: true,
        grid: {
          display:true
        },
        ticks: {
          callback: function(value: any) {
            return value + ' GB'
          }
        }
      },
      y: {
        stacked: true,
        grid: {
          display:false
        },
        title: {
          display: true,
          align: 'center',
          text: 'Host'
        }
      }
    }
  }
})

//Potentially handled by BE
const addValues = (a: number, b: number) => {
  const total = (a + b).toString()
  return (parseFloat(total).toPrecision(3))
}
const postFixGB = (value: any) => value + ' GB'



</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins.scss';
@use '@featherds/styles/mixins/typography';
@import "@featherds/table/scss/table";

.table-chart-container{
  display: flex;
  justify-content: flex-start;
  align-items: stretch;
  align-content: center;
  flex-direction: row;
  min-width: 0;
  flex-wrap: wrap;
  .table-container{
    flex: 1 1 0;
  }
  .chart-container{
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
  th, td {
    border-bottom: 0px;
    box-shadow: none;
  }
}
</style>
