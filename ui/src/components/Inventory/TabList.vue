<template>
  <FeatherTabContainer class="tab-container">
    <template v-slot:tabs>
      <FeatherTab v-for="tab in tabs" :key="tab.label">{{ tab.label }}</FeatherTab>
    </template>
    <Filter />
    <FeatherTabPanel v-for="tab in tabs" :key="tab.label">
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
import { NodeDetailContentType } from '@/types'
import { TabNode } from '@/types/inventory'

const tabs: TabNode[] = [
  {
    type: NodeDetailContentType.MONITORED,
    label: 'Monitored Nodes',
    nodes: [
      {
        header: 'Node 1'
      },
      {
        header: 'Node 11'
      }
    ]
  },
  {
    type: NodeDetailContentType.MONITORED,
    label: 'Unmonitored Nodes',
    nodes: [
      {
        header: 'Node 2'
      },
      {
        header: 'Node 22'
      }
    ]
  },
  {
    type: NodeDetailContentType.DETECTED,
    label: 'Detected Nodes',
    nodes: [
      {
        header: 'Node 3'
      },
      {
        header: 'Node 33'
      }
    ]
  }
]
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';

.tab-container {
  :deep(> ul) {
    display: flex;
    border-bottom: 1px solid var(variables.$disabled-text-on-surface);
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