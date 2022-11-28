<template>
  <ul class="icon-action-list">
    <IconAction :item="bubbleChartItem" :asLi="true" />
    <IconAction :item="lineChartItem" :asLi="true" />
    <IconAction :item="pieChartItem" :asLi="true" />
    <IconAction :item="warningItem" :asLi="true" />
    <IconAction :item="deleteItem" :asLi="true" />
  </ul>
</template>

<script lang="ts" setup>
import BubbleChart from '@material-design-icons/svg/outlined/bubble_chart.svg'
import MultilineChart from '@material-design-icons/svg/outlined/multiline_chart.svg'
import PieChart from '@material-design-icons/svg/outlined/pie_chart.svg'
import Warning from '@featherds/icon/notification/Warning'
import Delete from '@featherds/icon/action/Delete'
import IconAction from '@/components/Common/IconAction.vue'

const onBubbleChart = () => {
  console.log('bubble chart')
}
const bubbleChartItem = {
  icon: BubbleChart,
  title: 'Bubble Chart',
  action: onBubbleChart
}

const onLineChart = () => {
  console.log('line chart')
}
const lineChartItem = {
  icon: MultilineChart,
  title: 'Line Chart',
  action: onLineChart
}

const onPieChart = () => {
  console.log('pie chart')
}
const pieChartItem = {
  icon: PieChart,
  title: 'Pie Chart',
  action: onPieChart
}

const onWarning = () => {
  console.log('warning')
}
const warningItem = {
  icon: markRaw(Warning),
  title: 'Warning',
  action: onWarning
}

const onDelete = () => {
  console.log('delete')
}
const deleteItem = {
  icon: markRaw(Delete),
  title: 'Delete',
  action: onDelete
}
</script>

<style lang="scss" scoped>
.icon-action-list {
  list-style-type: none;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.2rem;
}
</style>