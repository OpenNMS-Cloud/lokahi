<template>
  <HeadlinePage
    text="Network Inventory"
    class="header"
    data-test="page-header"
  />
  <FeatherTabContainer
    class="tab-container"
    data-test="tab-container"
  >
    <template v-slot:tabs>
      <FeatherTab @click="inventoryQueries.getMonitoredNodes">Monitored Nodes</FeatherTab>
      <FeatherTab @click="inventoryQueries.getUnmonitoredNodes">Unmonitored Nodes</FeatherTab>
      <FeatherTab @click="inventoryQueries.getDetectedNodes">Detected Nodes</FeatherTab>
    </template>

    <!-- Monitored Nodes -->
    <FeatherTabPanel>
      <InventoryFilter />
      <InventoryTabContent
        v-if="tabMonitoredContent.length"
        :tabContent="tabMonitoredContent"
        :state="MonitoredStates.MONITORED"
      />
    </FeatherTabPanel>

    <!-- Unmonitored Nodes -->
    <FeatherTabPanel>
      <InventoryFilter onlyTags/>
      <InventoryTabContent
        v-if="tabUnmonitoredContent.length"
        :tabContent="tabUnmonitoredContent"
        :state="MonitoredStates.UNMONITORED"
      />
    </FeatherTabPanel>

    <!-- Detected Nodes -->
    <FeatherTabPanel>
      <InventoryFilter onlyTags />
      <InventoryTabContent
        v-if="tabDetectedContent.length"
        :tabContent="tabDetectedContent"
        :state="MonitoredStates.DETECTED"
      />
    </FeatherTabPanel>
  </FeatherTabContainer>
</template>

<script lang="ts" setup>
import { FeatherTab, FeatherTabContainer, FeatherTabPanel } from '@featherds/tabs'
import InventoryFilter from '@/components/Inventory/InventoryFilter.vue'
import InventoryTabContent from '@/components/Inventory/InventoryTabContent.vue'
import { MonitoredNode, UnmonitoredNode, DetectedNode, MonitoredStates } from '@/types'
import { useInventoryQueries } from '@/store/Queries/inventoryQueries'

const inventoryQueries = useInventoryQueries()
const tabMonitoredContent = computed((): MonitoredNode[] => inventoryQueries.nodes)
const tabUnmonitoredContent = computed((): UnmonitoredNode[] => inventoryQueries.unmonitoredNodes)
const tabDetectedContent = computed((): DetectedNode[] => inventoryQueries.detectedNodes)

onMounted(() => inventoryQueries.getMonitoredNodes())
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
