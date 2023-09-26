<template>
  <div class="full-page-container">
    <div class="flex">
      <HeadlinePage
        text="Network Inventory"
        class="header"
        data-test="page-header"
      />
    </div>

    <InventoryTagModal
      :visible="tagStore.isVisible"
      :title="''"
      :node="tagStore.activeNode"
      :closeModal="tagStore.closeModal"
    />
    <FeatherTabContainer
      class="tab-container"
      data-test="tab-container"
    >
      <template v-slot:tabs>
        <FeatherTab
          >Monitored Nodes
          <FeatherTextBadge
            :type="BadgeTypes.info"
            v-if="tabMonitoredContent.length > 0"
            >{{ tabMonitoredContent.length }}</FeatherTextBadge
          >
        </FeatherTab>
        <FeatherTab
          >Unmonitored Nodes
          <FeatherTextBadge
            :type="BadgeTypes.info"
            v-if="tabUnmonitoredContent.length > 0"
            >{{ tabUnmonitoredContent.length }}</FeatherTextBadge
          >
        </FeatherTab>
        <FeatherTab
          >Detected Nodes
          <FeatherTextBadge
            :type="BadgeTypes.info"
            v-if="tabDetectedContent.length > 0"
            >{{ tabDetectedContent.length }}</FeatherTextBadge
          >
        </FeatherTab>
      </template>
      <!-- Monitored Nodes -->
      <FeatherTabPanel>
        <InventoryFilter
          v-if="inventoryStore.monitoredFilterActive"
          :state="MonitoredStates.MONITORED"
          :nodes="tabMonitoredContent"
        />
        <InventoryTabContent
          v-if="tabMonitoredContent.length && !inventoryStore.loading"
          :tabContent="tabMonitoredContent"
          :state="MonitoredStates.MONITORED"
        />
        <EmptyList
          data-test="monitored-empty"
          v-if="!tabMonitoredContent.length && !inventoryStore.loading"
          bg
          :content="{
            msg: 'No monitored nodes. Add some on the Discovery page.',
            btn: {
              label: 'Visit Discovery Page',
              action: () => {
                $router.push('/discovery')
              }
            }
          }"
        />
        <FeatherSpinner v-if="inventoryStore.loading" />
      </FeatherTabPanel>

      <!-- Unmonitored Nodes -->
      <FeatherTabPanel>
        <InventoryFilter
          v-if="inventoryStore.unmonitoredFilterActive"
          :state="MonitoredStates.UNMONITORED"
          onlyTags
          :nodes="tabUnmonitoredContent"
        />
        <InventoryTabContent
          v-if="tabUnmonitoredContent.length && !inventoryStore.loading"
          :tabContent="tabUnmonitoredContent"
          :state="MonitoredStates.UNMONITORED"
        />
        <EmptyList
          data-test="unmonitored-empty"
          v-if="!tabUnmonitoredContent.length && !inventoryStore.loading"
          bg
          :content="{
            msg: 'No unmonitored nodes. Add some on the Discovery page.',
            btn: {
              label: 'Visit Discovery Page',
              action: () => {
                $router.push('/discovery')
              }
            }
          }"
        />
        <FeatherSpinner v-if="inventoryStore.loading" />
      </FeatherTabPanel>

      <!-- Detected Nodes -->
      <FeatherTabPanel>
        <InventoryFilter
          v-if="inventoryStore.detectedFilterActive"
          :state="MonitoredStates.DETECTED"
          onlyTags
          :nodes="tabDetectedContent"
        />
        <InventoryTabContent
          v-if="tabDetectedContent.length && !inventoryStore.loading"
          :tabContent="tabDetectedContent"
          :state="MonitoredStates.DETECTED"
        />
        <EmptyList
          data-test="discovery-empty"
          v-if="!tabDetectedContent.length && !inventoryStore.loading"
          bg
          :content="{
            msg: 'No detected nodes. Add some on the Discovery page.',
            btn: {
              label: 'Visit Discovery Page',
              action: () => {
                $router.push('/discovery')
              }
            }
          }"
        />
        <FeatherSpinner v-if="inventoryStore.loading" />
      </FeatherTabPanel>
    </FeatherTabContainer>
  </div>
</template>

<script lang="ts" setup>
import { FeatherTab, FeatherTabContainer, FeatherTabPanel } from '@featherds/tabs'
import InventoryFilter from '@/components/Inventory/InventoryFilter.vue'
import InventoryTabContent from '@/components/Inventory/InventoryTabContent.vue'
import { MonitoredStates, InventoryItem } from '@/types'
import { useInventoryStore } from '@/store/Views/inventoryStore'
import { FeatherTextBadge, BadgeTypes } from '@featherds/badge'
import { useTagStore } from '@/store/Components/tagStore'

const inventoryStore = useInventoryStore()
const tabMonitoredContent = computed((): InventoryItem[] =>
  inventoryStore.nodes.filter((d) => d.monitoredState === MonitoredStates.MONITORED)
)
const tabUnmonitoredContent = computed((): InventoryItem[] =>
  inventoryStore.nodes.filter((d) => d.monitoredState === MonitoredStates.UNMONITORED)
)
const tabDetectedContent = computed((): InventoryItem[] =>
  inventoryStore.nodes.filter((d) => d.monitoredState === MonitoredStates.DETECTED)
)
const tagStore = useTagStore()

onMounted(async () => {
  inventoryStore.init()
})

/**
 * If at any point, a tab of content is more than zero,
 * show the corresponding filter. Prior to this change, it
 * was hidden on zero results, which meant hidden when no search results.
 */
watchEffect(() => {
  if (tabMonitoredContent.value.length > 0) {
    inventoryStore.monitoredFilterActive = true
  }
  if (tabUnmonitoredContent.value.length > 0) {
    inventoryStore.unmonitoredFilterActive = true
  }
  if (tabDetectedContent.value.length > 0) {
    inventoryStore.detectedFilterActive = true
  }
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';

.tab-container {
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
