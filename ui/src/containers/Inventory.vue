<template>
  <div class="flex">
    <HeadlinePage text="Network Inventory" class="header" data-test="page-header" />
  </div>

  <FeatherTabContainer class="tab-container" data-test="tab-container">
    <template v-slot:tabs>
      <FeatherTab @click="changeToTab(MonitoredState.Monitored)">Monitored Nodes
        <FeatherTextBadge :type="BadgeTypes.info" v-if="inventoryQueries.monitoredNodes.length > 0">
          {{ inventoryQueries.monitoredNodes.length }}
        </FeatherTextBadge>
      </FeatherTab>
      <FeatherTab @click="changeToTab(MonitoredState.Unmonitored)">Unmonitored Nodes
        <FeatherTextBadge :type="BadgeTypes.info" v-if="inventoryQueries.unmonitoredNodes.length > 0">
          {{ inventoryQueries.unmonitoredNodes.length }}
        </FeatherTextBadge>
      </FeatherTab>
      <FeatherTab @click="changeToTab(MonitoredState.Detected)">Detected Nodes
        <FeatherTextBadge :type="BadgeTypes.info" v-if="inventoryQueries.detectedNodes.length > 0">
          {{ inventoryQueries.detectedNodes.length }}
        </FeatherTextBadge>
      </FeatherTab>
    </template>
    <!-- Monitored Nodes -->
    <FeatherTabPanel>
      <InventoryFilter v-if="inventoryStore.monitoredFilterActive" :nodes="tabMonitoredContent" />
      <FeatherSpinner v-if="inventoryQueries.isFetching" />
      <template v-else>
        <InventoryTabContent v-if="tabMonitoredContent.length" :tabContent="tabMonitoredContent"
          :state="MonitoredState.Monitored" />
        <EmptyList data-test="monitored-empty" v-if="!tabMonitoredContent.length" bg
          :content="{ msg: 'No monitored nodes. Add some on the Discovery page.', btn: { label: 'Visit Discovery Page', action: () => { $router.push('/discovery') } } }" />
      </template>
    </FeatherTabPanel>

    <!-- Unmonitored Nodes -->
    <FeatherTabPanel>
      <InventoryFilter v-if="inventoryStore.unmonitoredFilterActive" onlyTags :nodes="tabUnmonitoredContent" />
      <FeatherSpinner v-if="inventoryQueries.isFetching" />
      <template v-else>
        <InventoryTabContent v-if="tabUnmonitoredContent.length" :tabContent="tabUnmonitoredContent"
          :state="MonitoredState.Unmonitored" />
        <EmptyList data-test="unmonitored-empty" v-if="!tabUnmonitoredContent.length" bg
          :content="{ msg: 'No unmonitored nodes. Add some on the Discovery page.', btn: { label: 'Visit Discovery Page', action: () => { $router.push('/discovery') } } }" />
      </template>
    </FeatherTabPanel>

    <!-- Detected Nodes -->
    <FeatherTabPanel>
      <InventoryFilter v-if="inventoryStore.detectedFilterActive" onlyTags :nodes="tabDetectedContent" />
      <FeatherSpinner v-if="inventoryQueries.isFetching" />
      <template v-else>
        <InventoryTabContent v-if="tabDetectedContent.length" :tabContent="tabDetectedContent"
          :state="MonitoredState.Detected" />
        <EmptyList data-test="discovery-empty" v-if="!tabDetectedContent.length" bg
          :content="{ msg: 'No detected nodes. Add some on the Discovery page.', btn: { label: 'Visit Discovery Page', action: () => { $router.push('/discovery') } } }" />
      </template>
    </FeatherTabPanel>
  </FeatherTabContainer>
</template>

<script lang="ts" setup>
import { FeatherTab, FeatherTabContainer, FeatherTabPanel } from '@featherds/tabs'
import InventoryFilter from '@/components/Inventory/InventoryFilter.vue'
import InventoryTabContent from '@/components/Inventory/InventoryTabContent.vue'
import { MonitoredNode, UnmonitoredNode, DetectedNode } from '@/types'
import { useInventoryQueries } from '@/store/Queries/inventoryQueries'
import { useInventoryStore } from '@/store/Views/inventoryStore'
import { FeatherTextBadge, BadgeTypes } from '@featherds/badge'
import { MonitoredState } from '@/types/graphql'

const inventoryStore = useInventoryStore()
const inventoryQueries = useInventoryQueries()
const tabMonitoredContent = computed((): MonitoredNode[] => inventoryQueries.monitoredNodes)
const tabUnmonitoredContent = computed((): UnmonitoredNode[] => inventoryQueries.unmonitoredNodes)
const tabDetectedContent = computed((): DetectedNode[] => inventoryQueries.detectedNodes)

onMounted(() => {
  inventoryQueries.getMonitoredNodes()
  inventoryQueries.getUnmonitoredNodes()
  inventoryQueries.getDetectedNodes()
})

const changeToTab = (monitoredState: MonitoredState) => {
  inventoryStore.selectedMonitoredState = monitoredState
  inventoryStore.resetSearch()
  inventoryQueries.fetchByState(inventoryStore.selectedMonitoredState)
}

/**
 * If at any point, a tab of content is more than zero, 
 * show the corresponding filter. Prior to this change, it 
 * was hidden on zero results, which meant hidden when no search results.
 */
watchEffect(() => {
  if (inventoryQueries.monitoredNodes.length > 0) {
    inventoryStore.monitoredFilterActive = true
  }
  if (inventoryQueries.unmonitoredNodes.length > 0) {
    inventoryStore.unmonitoredFilterActive = true
  }
  if (inventoryQueries.detectedNodes.length > 0) {
    inventoryStore.detectedFilterActive = true
  }
})

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

    >li {
      display: flex !important;
      flex-grow: 1;

      >button {
        display: flex;
        flex-grow: 1;

        >span {
          flex-grow: 1;
        }
      }
    }
  }
}

.flex {
  display: flex;
  width: 100%;
  justify-content: space-between;
  align-items: center;
}

.tag-manager {
  margin-right: 24px;
}

.padding {
  padding: 24px;
  transition: padding ease-in-out 0.4s;
}

.no-padding {
  padding: 24px;
  padding-top: 0;
  padding-bottom: 0;
  transition: padding ease-in-out 0.4s;
}
</style>
