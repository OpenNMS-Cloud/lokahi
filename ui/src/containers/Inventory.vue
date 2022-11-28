<template>
  <PageHeader :heading="'Network Inventory'" class="mx-l" />
  <FeatherTabContainer class="tab-container">
    <template v-slot:tabs>
      <FeatherTab v-for="tab in tabs" :key="tab.label">{{ tab.label }}</FeatherTab>
    </template>
    <FeatherTabPanel v-for="tab in tabs" :key="tab.label">
      <Filter />
      <DetectedNodesTabContent v-if="tab.type === NodeDetailContentType.DETECTED" :tab="tab" />
      <MonitoredNodesTabContent v-else :tab="tab" />
    </FeatherTabPanel>
  </FeatherTabContainer>
</template>

<script lang="ts" setup>
import {
  FeatherTab,
  FeatherTabContainer,
  FeatherTabPanel
} from '@featherds/tabs'
import Filter from '@/components/Inventory/Filter.vue'
import DetectedNodesTabContent from '@/components/Inventory/DetectedNodesTabContent.vue'
import MonitoredNodesTabContent from '@/components/Inventory/MonitoredNodesTabContent.vue'
import { NodeDetailContentType, TimeUnit } from '@/types'
import { TabNode } from '@/types/inventory'

const tabs: TabNode[] = [
  {
    type: NodeDetailContentType.MONITORED,
    label: 'Monitored Nodes',
    nodes: [
      {
        id: 1,
        name: 'Monitored Node 1',
        metrics: [
          {
            type: 'latency',
            timestamp: 9,
            timeUnit: TimeUnit.MSecs,
            status: 'UP'
          },
          {
            type: 'uptime',
            timestamp: 1667930274.660,
            timeUnit: TimeUnit.Secs,
            status: 'DOWN'
          },
          {
            type: 'status',
            status: 'DOWN'
          }
        ],
        anchor: {
          profileValue: 75,
          profileLink: 'goto',
          locationValue: 'DefaultMinion',
          locationLink: 'goto',
          ipInterfaceValue: 25,
          ipInterfaceLink: 'goto',
          tagValue: 100,
          tagLink: 'goto'
        }
      },
      {
        id: 2,
        name: 'Monitored Node 2',
        metrics: [
          {
            type: 'latency',
            timestamp: 9,
            timeUnit: TimeUnit.MSecs,
            status: 'UP'
          },
          {
            type: 'uptime',
            timestamp: 1667930274.660,
            timeUnit: TimeUnit.Secs,
            status: 'DOWN'
          },
          {
            type: 'status',
            status: 'DOWN'
          }
        ],
        anchor: {
          profileValue: 75,
          profileLink: 'goto',
          locationValue: 'DefaultMinion',
          locationLink: 'goto',
          ipInterfaceValue: 25,
          ipInterfaceLink: 'goto',
          tagValue: 100,
          tagLink: 'goto'
        }
      },
      {
        id: 3,
        name: 'Monitored Node 3',
        metrics: [
          {
            type: 'latency',
            timestamp: 9,
            timeUnit: TimeUnit.MSecs,
            status: 'UP'
          },
          {
            type: 'uptime',
            timestamp: 1667930274.660,
            timeUnit: TimeUnit.Secs,
            status: 'DOWN'
          },
          {
            type: 'status',
            status: 'DOWN'
          }
        ],
        anchor: {
          profileValue: 75,
          profileLink: 'goto',
          locationValue: 'DefaultMinion',
          locationLink: 'goto',
          ipInterfaceValue: 25,
          ipInterfaceLink: 'goto',
          tagValue: 100,
          tagLink: 'goto'
        }
      },
      {
        id: 4,
        name: 'Monitored Node 4',
        metrics: [
          {
            type: 'latency',
            timestamp: 9,
            timeUnit: TimeUnit.MSecs,
            status: 'UP'
          },
          {
            type: 'uptime',
            timestamp: 1667930274.660,
            timeUnit: TimeUnit.Secs,
            status: 'DOWN'
          },
          {
            type: 'status',
            status: 'DOWN'
          }
        ],
        anchor: {
          profileValue: 75,
          profileLink: 'goto',
          locationValue: 'DefaultMinion',
          locationLink: 'goto',
          ipInterfaceValue: 25,
          ipInterfaceLink: 'goto',
          tagValue: 100,
          tagLink: 'goto'
        }
      }
    ]
  }
]
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';

.tab-container {
  margin: 0 var(variables.$spacing-l);
  :deep(> ul) {
    display: flex;
    border-bottom: 1px solid var(variables.$secondary-text-on-surface);
    > li {
      display: flex !important;
      flex-grow: 1;
      > button {
        display: flex;
        flex-grow: 1;
        > span {
          flex-grow: 1;
        }
      }
    }
  }
}
</style>