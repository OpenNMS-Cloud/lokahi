<template>
  <TableCard>
    <div class="header">
      <div class="title-container">
        <FeatherButton
          data-test="show-minions-btn"
          icon="Show Minions" 
          @click="appliancesStore.showMinionsTable"
          v-if="!appliancesStore.minionsTableOpen"
        >
          <FeatherIcon :icon="ChevronRight" />
        </FeatherButton>
        <span class="title">Devices ({{nodesTable.length}})</span>
      </div>

      <FeatherInput
        class="search" 
        v-model="searchValue" 
        label="Devices">
        <template v-slot:pre>
          <FeatherIcon :icon="Search" />
        </template>
      </FeatherInput>

      <div class="btns">
        <FeatherButton icon="Filter">
          <FeatherIcon :icon="FilterAlt" />
        </FeatherButton>
        <FeatherButton icon="Sort">
          <FeatherIcon :icon="Sort" />
        </FeatherButton>
      </div>

    </div>
    <div class="data-table">
      <TransitionGroup name="data-table" tag="div">
        <div class="card" v-for="(node) in nodesTable" :key="(node.id as number)" data-test="node-item">
          <div class="name pointer" @click="gotoNode(node.id as number)" data-test="col-node">
              <div class="name-cell">
                <FeatherIcon :icon="Instances" class="icon"/>
                <div class="text">
                  <div class="name">{{ node.nodeLabel }}</div>
                  <div class="server" v-date>{{ node.createTime }}</div>
                </div>
              </div>
          </div>
          <div />
          <MetricChip :metric="{value: node.latency?.value, label: 'ICMP Latency'}" @click="openLatencyGraph(node)" :data-metric="node.latency?.value" class="bg-status pointer" data-test="node-item-latency" />
          <MetricChip :metric="{label: 'Status', status: node.status}" class="bg-status" data-test="node-item-status" />
        </div>
      </TransitionGroup>
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
import FilterAlt from '@featherds/icon/action/FilterAlt'
import Sort from '@featherds/icon/action/Sort'
import Search from '@featherds/icon/action/Search'
import Instances from '@featherds/icon/hardware/Instances'
import ChevronRight from '@featherds/icon/navigation/ChevronRight'
import { useAppliancesQueries } from '@/store/Queries/appliancesQueries'
import { useAppliancesStore } from '@/store/Views/appliancesStore'
import { ExtendedNode } from '@/types/node'
import { GraphProps } from '@/types/graphs'
import { TimeRangeUnit } from '@/types/graphql'
import { AZURE_SCAN, Monitor } from '@/types'

const appliancesStore = useAppliancesStore()
const appliancesQueries = useAppliancesQueries()
const router = useRouter()

const nodesTable = computed<ExtendedNode[]>(() => appliancesQueries.tableNodes)

const searchValue = ref('')

const gotoNode = (nodeId: number) => router.push(`/node/${nodeId}`)

const graphProps = ref({} as GraphProps)
const modal = ref({
  isVisible: false,
  title: '',
  hideTitle: true
})
const openLatencyGraph = (node: ExtendedNode) => {
  modal.value = {
    ...modal.value,
    isVisible: true
  }
  graphProps.value = {
    label: 'Device Latency',
    metrics: ['response_time_msec'],
    monitor: node.scanType === AZURE_SCAN ? Monitor.AZURE : Monitor.ICMP,
    nodeId: node.id,
    instance: node.scanType === AZURE_SCAN ? 'azure-node-' + node.id : node.ipInterfaces?.[0].ipAddress as string, // currently 1 interface per node
    timeRange: 10,
    timeRangeUnit: TimeRangeUnit.Minute
  }
}
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";
@use "@featherds/styles/mixins/typography";
@use "@/styles/_transitionDataTable";
@use "@/styles/_statusBackground";

.header {
  display: flex;
  justify-content: space-between;

  .title-container {
    display: flex;
    .title {
      @include typography.headline3;
      margin-left: 15px;
      margin-top: 2px;
    }
  }
  
  .search {
    width: 300px;
  }

  .btns {
    display: flex;
  }
}

:deep(.chip) {
  .label {
    margin: 0 auto;
  }
}

.card {
  border: 1px solid var(variables.$shade-4);
  display: flex;
  margin-bottom: 10px;
  border-radius: 5px;
  height: 65px;
  justify-content: space-between;    
  
  div {
    display: flex;
    flex-direction: column;
    justify-content: center;
    width: 20%;
    padding: 8px;
    line-height: 15px;
    font-size: 11px;

    &.name {
      @include typography.subtitle1;
      width: 40%;
      color: var(variables.$primary);

      .name-cell {
        flex-direction: row;
        width: 100%;
        justify-content: flex-start;
        white-space: nowrap;
        align-items: center;
        .icon {
          font-size: 25px;
          color: var(variables.$shade-2);
        }

        .text {
          flex-direction: column;
          width: 100%;
          .name {
            font-size: 15px;
            line-height: 0px;
          }
          .server {
            line-height: 10px;
            color: var(variables.$secondary)
          }
        }
      }
    }

    .title {
      font-family: inherit;
      margin: 0px;
    }

    .value {
      display: inline-table;
      border-radius: 5px;
      padding: 3px 10px;
      text-align: center;
      white-space: nowrap;
    }
  }
}
</style>
