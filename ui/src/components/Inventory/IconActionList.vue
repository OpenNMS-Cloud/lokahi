<template>
  <ul class="icon-action-list">
    <li @click="onBubbleChart" data-test="bubble-chart"><Icon :icon="bubbleChartIcon" /></li>
    <li @click="onLineChart" data-test="line-chart"><Icon :icon="lineChartIcon" /></li>
    <li @click="onPieChart" data-test="pie-chart"><Icon :icon="pieChartIcon" /></li>
    <li @click="onWarning" data-test="warning"><Icon :icon="warningIcon" /></li>
    <li @click="onDelete" data-test="delete"><Icon :icon="deleteIcon" /></li>
  </ul>
</template>

<script lang="ts" setup>
import BubbleChart from '@material-design-icons/svg/outlined/bubble_chart.svg'
import MultilineChart from '@material-design-icons/svg/outlined/multiline_chart.svg'
import PieChart from '@material-design-icons/svg/outlined/pie_chart.svg'
import Warning from '@featherds/icon/notification/Warning'
import Delete from '@featherds/icon/action/Delete'
import { IIcon } from '@/types'

const onBubbleChart = () => {
  console.log('bubble chart')
}
const bubbleChartIcon: IIcon = {
  image: BubbleChart,
  title: 'Bubble Chart'
}

const onLineChart = () => {
  console.log('line chart')
}
const lineChartIcon: IIcon = {
  image: MultilineChart,
  title: 'Line Chart'
}

const onPieChart = () => {
  console.log('pie chart')
}
const pieChartIcon: IIcon = {
  image: PieChart,
  title: 'Pie Chart'
}

const onWarning = () => {
  console.log('warning')
}
const warningIcon: IIcon = {
  image: markRaw(Warning),
  title: 'Warning'
}

const onDelete = () => {
  console.log('delete')
}
const deleteIcon: IIcon = {
  image: markRaw(Delete),
  title: 'Delete'
}
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";

ul.icon-action-list {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.2rem;
  > li {
    padding: var(variables.$spacing-xxs);
    font-size: 1.5rem;
    color: var(variables.$secondary-text-on-surface);
    &:hover {
      color: var(variables.$disabled-text-on-surface);
    }
  }
}
</style>