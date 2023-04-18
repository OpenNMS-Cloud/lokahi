<template>
  <HeadlinePage
    :text="heading"
    class="header"
    data-test="page-header"
  />
  <FeatherTabContainer
    class="tab-container"
    data-test="tab-container"
  >
    <template v-slot:tabs>
      <FeatherTab
        v-for="tab in tabs"
        :key="tab.label"
        >{{ tab.label }}</FeatherTab
      >
    </template>
    <FeatherTabPanel>
      <InventoryFilter />
      <InventoryMonitoredNodesTabContent
        v-if="tabMonitoredContent.length"
        :tabContent="tabMonitoredContent"
      />
    </FeatherTabPanel>
    <!-- <FeatherTabPanel>
      <InventoryFilter v-if="tabUnmonitoredContent.length" />
      <InventoryMonitoredNodesTabContent :tabContent="tabUnmonitoredContent" />
    </FeatherTabPanel> -->
    <!-- <FeatherTabPanel>
      <InventoryFilter v-if="tabDetectedContent.length" />
      <InventoryDetectedNodesTabContent :tabContent="tabDetectedContent" />
    </FeatherTabPanel> -->
  </FeatherTabContainer>
</template>

<script lang="ts" setup>
import { FeatherTab, FeatherTabContainer, FeatherTabPanel } from '@featherds/tabs'
import InventoryFilter from '@/components/Inventory/InventoryFilter.vue'
import InventoryMonitoredNodesTabContent from '@/components/Inventory/InventoryMonitoredNodesTabContent.vue'
// import InventoryDetectedNodesTabContent from '@/components/Inventory/InventoryDetectedNodesTabContent.vue'
// import { TimeUnit } from '@/types'
import { NodeContent } from '@/types/inventory'
import { useInventoryQueries } from '@/store/Queries/inventoryQueries'

/* const unmonitoredContent: NodeContent[] = [
  {
    id: 1,
    label: 'Unmonitored Node 1',
    status: '',
    metrics: [
      {
        type: 'latency',
        label: 'Latency',
        timestamp: 9,
        timeUnit: TimeUnit.MSecs,
        status: 'UP'
      },
      {
        type: 'uptime',
        label: 'Uptime',
        timestamp: 1667930274.660,
        timeUnit: TimeUnit.Secs,
        status: '--'
      },
      {
        type: 'status',
        label: 'Status',
        status: 'DOWN'
      }
    ],
    anchor: {
      profileValue: 70,
      profileLink: 'goto',
      locationValue: 'Default',
      locationLink: 'goto',
      managementIpValue: '0.0.0.0',
      managementIpLink: 'goto',
      tagValue: 10,
      tagLink: 'goto'
    }
  },
  {
    id: 2,
    label: 'Unmonitored Node 2',
    status: '',
    metrics: [
      {
        type: 'latency',
        label: 'Latency',
        timestamp: 89,
        timeUnit: TimeUnit.MSecs,
        status: 'UP'
      },
      {
        type: 'uptime',
        label: 'Uptime',
        timestamp: 1667930274.660,
        timeUnit: TimeUnit.Secs,
        status: 'DOWN'
      },
      {
        type: 'status',
        label: 'Status',
        status: '--'
      }
    ],
    anchor: {
      profileValue: 30,
      profileLink: 'goto',
      locationValue: 'DefaultMinion',
      locationLink: 'goto',
      managementIpValue: '0.0.0.0',
      managementIpLink: 'goto',
      tagValue: 49,
      tagLink: 'goto'
    }
  }
] */
/* const detectedContent: NodeContent[] = [
  {
    id: 1,
    label: 'Detected Node 1',
    status: '',
    metrics: [
      {
        type: 'latency',
        label: 'Latency',
        timestamp: '--',
        timeUnit: TimeUnit.MSecs,
        status: '--'
      },
      {
        type: 'uptime',
        label: 'Uptime',
        timestamp: 1667930274.660,
        timeUnit: TimeUnit.Secs,
        status: 'UP'
      },
      {
        type: 'status',
        label: 'Status',
        status: 'DOWN'
      }
    ],
    anchor: {
      profileValue: 40,
      profileLink: 'goto',
      locationValue: 'DefaultMinion',
      locationLink: 'goto',
      managementIpValue: '10.0.0.1',
      managementIpLink: 'goto',
      tagValue: 20,
      tagLink: 'goto'
    }
  },
  {
    id: 2,
    label: 'Detected Node 2',
    status: '',
    metrics: [
      {
        type: 'latency',
        label: 'Latency',
        timestamp: 50,
        timeUnit: TimeUnit.MSecs,
        status: 'DOWN'
      },
      {
        type: 'uptime',
        label: 'Uptime',
        timestamp: 1667930274.660,
        timeUnit: TimeUnit.Secs,
        status: 'UP'
      },
      {
        type: 'status',
        label: 'Status',
        status: '--'
      }
    ],
    anchor: {
      profileValue: 53,
      profileLink: 'goto',
      locationValue: 'DefaultMinion',
      locationLink: 'goto',
      managementIpValue: '0.0.0.0',
      managementIpLink: 'goto',
      tagValue: 60,
      tagLink: 'goto'
    }
  },
  {
    id: 3,
    label: 'Detected Node 3',
    status: '',
    metrics: [
      {
        type: 'latency',
        label: 'Latency',
        timestamp: 19,
        timeUnit: TimeUnit.MSecs,
        status: 'UP'
      },
      {
        type: 'uptime',
        label: 'Uptime',
        timestamp: 1667930274.660,
        timeUnit: TimeUnit.Secs,
        status: '--'
      },
      {
        type: 'status',
        label: 'Status',
        status: '--'
      }
    ],
    anchor: {
      profileValue: 40,
      profileLink: 'goto',
      locationValue: 'DefaultMinion',
      locationLink: 'goto',
      managementIpValue: '11.1.1.1',
      managementIpLink: 'goto',
      tagValue: 62,
      tagLink: 'goto'
    }
  }
] */

const heading = 'Network Inventory'
const tabs = [{ label: 'Monitored Nodes' }, { label: 'Unmonitored Nodes' }, { label: 'Detected Nodes' }]

const inventoryQueries = useInventoryQueries()
const tabMonitoredContent = computed((): NodeContent[] => inventoryQueries.nodes)

// const tabUnmonitoredContent = unmonitoredContent
// const tabDetectedContent = detectedContent
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';

.header {
  margin-right: var(variables.$spacing-l);
  margin-left: var(variables.$spacing-l);
}

.tab-container {
  margin: 0 var(variables.$spacing-l);
  :deep(> ul) {
    display: flex;
    border-bottom: 1px solid var(variables.$secondary-text-on-surface);
    min-width: vars.$min-width-smallest-screen;
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
