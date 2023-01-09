<template>
  <TableCard v-if="appliancesStore.minionsTableOpen">
    <div class="header">
      <div class="title">
        Minions ({{ applianceQueries.tableMinions.length }})
      </div>
      <FeatherButton 
        data-test="hide-minions-btn"
        icon="Hide Minions" 
        @click="appliancesStore.hideMinionsTable"
        v-if="!widgetProps?.isWidget"
      >
        <FeatherIcon :icon="ChevronLeft" />
      </FeatherButton>
    </div>
    <div class="container">
      <table class="tl1 tl2 tl3 tc4 tc5 tc6 data-table" aria-label="Minions Table" data-test="minions-table">
        <thead>
          <tr>
            <th scope="col" data-test="col-label">Label</th>
            <th scope="col" data-test="col-date">Time</th>
            <th scope="col" data-test="col-minion">Id</th>
            <th scope="col" data-test="col-latency">Latency</th>
            <th scope="col" data-test="col-status">Status</th>
          </tr>
        </thead>
        <TransitionGroup name="data-table" tag="tbody">
          <tr v-for="(minion, index) in minionsTable" :key="(minion.id as string)" :data-index="index" data-test="minion-item">
            <td>{{ minion.label }}</td>
            <td v-date>{{ minion.lastCheckedTime }}</td>
            <td>{{ minion.id }}</td>
            <MetricChip tag="td" :metric="{timestamp: minion.latency?.timestamp}" :data-metric="minion.latency?.timestamp" class="bg-status" data-test="minion-item-latency" />
            <MetricChip tag="td" :metric="{status: minion.status}" class="bg-status" data-test="minion-item-status" />
          </tr>
        </TransitionGroup>
      </table>
    </div>
  </TableCard>
  <PrimaryModal :visible="modal.isVisible" :title="modal.title" :hide-title="modal.hideTitle">
    <template #content>
      <LineGraph :graph="graphProps" />
    </template>
    <template #footer>
      <FeatherButton primary @click="modal.isVisible = false">Close</FeatherButton>
    </template>
  </PrimaryModal>
</template>

<script setup lang="ts">
import { useAppliancesQueries } from '@/store/Queries/appliancesQueries'
import { useAppliancesStore } from '@/store/Views/appliancesStore'
import ChevronLeft from '@featherds/icon/navigation/ChevronLeft'
import { Monitor, WidgetProps} from '@/types'
import { GraphProps } from '@/types/graphs'
import { ExtendedMinion } from '@/types/minion'
import { TimeRangeUnit } from '@/types/graphql'
import MetricChip from '../Common/MetricChip.vue'

defineProps<{widgetProps?: WidgetProps}>()

const appliancesStore = useAppliancesStore()
const applianceQueries = useAppliancesQueries()
const minionsTable = computed<ExtendedMinion[]>(() => applianceQueries.tableMinions)

const graphProps = ref({} as GraphProps)
const modal = ref({
  isVisible: false,
  title: '',
  hideTitle: true
})

const openLatencyGraph = (minion: ExtendedMinion) => {
  modal.value = {
    ...modal.value,
    isVisible: true
  }
  graphProps.value = {
    label: 'Minion Latency',
    metrics: ['response_time_msec'],
    monitor: Monitor.ECHO,
    nodeId: minion.id,
    instance: minion.systemId as string, // for minions, can use systemId for instance
    timeRange: 10,
    timeRangeUnit: TimeRangeUnit.Minute
  }
}
</script>

<style lang="scss" scoped>
@use "@featherds/styles/mixins/typography";
@use "@featherds/table/scss/table";
@use "@/styles/_statusBackground";

.header {
  display: flex;
  justify-content: space-between;
  .title {
    @include typography.headline3;
    margin-left: 15px;
  }
}

.container {
  display: block;
  overflow-x: auto;

  table {
    width: 100%;
    @include table.table;
    @include table.table-condensed;
    thead {
      background: var(typography.$background);
      text-transform: uppercase;
    }
    td {
      white-space: nowrap;
      display: table-cell;
      div {
        border-radius: 5px;
        padding: 0px 5px 0px 5px;
      }
    }
  }
}
</style>
