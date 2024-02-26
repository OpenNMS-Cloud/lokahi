<template>
  <div class="full-page-container">
    <div class="header-wrapper">
      <div class="header">
        <div class="pre-title">Node Status</div>
        <div class="page-headline">
          {{ nodeStatusStore.node.nodeAlias || nodeStatusStore.node.nodeLabel }}
          <FeatherButton
            icon="Edit"
            @click="openModal"
          >
            <FeatherIcon :icon="EditIcon" />
          </FeatherButton>
        </div>
        <div
          class="post-title"
          v-if="nodeStatusStore.node.nodeAlias"
        >
          {{ nodeStatusStore.node.nodeLabel }}
        </div>
      </div>
      <div class="header-button">
        <FeatherButton
          primary
          @click="onManageTags"
          data-test="node-status-manage-tags-btn"
          >Manage Tags</FeatherButton
        >
      </div>
    </div>
    <FeatherTabContainer
      class="tab-container"
      data-test="tab-container"
    >
      <template v-slot:tabs>
        <FeatherTab
          >Status
          <!-- <FeatherTextBadge
            :type="BadgeTypes.info"
            v-if="tabMonitoredContent.length > 0"
            >{{ tabMonitoredContent.length }}</FeatherTextBadge
          > -->
        </FeatherTab>
        <FeatherTab
          >Interfaces
        </FeatherTab>
        <FeatherTab
          >Events
        </FeatherTab>
      </template>
      <!-- Status -->
      <FeatherTabPanel>
        <NodeStatusTabContent/>
        <!-- <NodeInfoTable /> -->
      </FeatherTabPanel>
      <!-- Interfaces -->
      <FeatherTabPanel>
        <SNMPInterfacesTable v-if="!nodeStatusStore.isAzure" />
        <div class="interfaces-spacer" />
        <IPInterfacesTable />
      </FeatherTabPanel>
      <!-- Events -->
      <FeatherTabPanel>
        <EventsTable />
      </FeatherTabPanel>
    </FeatherTabContainer>
  </div>
  <EditModal
    :isVisible="isVisible"
    :closeModal="() => closeModal()"
    :handler="(newAlias: string) => nodeStatusStore.updateNodeAlias(newAlias)"
    :callback="() => queries.fetchNodeStatus()"
    title="Edit Node Name"
    inputLabel="Type a new name"
    :currentValue="nodeStatusStore.node.nodeAlias || ''"
  />
</template>

<script setup lang="ts">
import EditIcon from '@featherds/icon/action/EditMode'
import useModal from '@/composables/useModal'
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { useInventoryStore } from '@/store/Views/inventoryStore'
import { useTagStore } from '@/store/Components/tagStore'

const nodeStatusStore = useNodeStatusStore()
const queries = useNodeStatusQueries()
const route = useRoute()
const { openModal, closeModal, isVisible } = useModal()
const inventoryStore = useInventoryStore()
const tagStore  = useTagStore()
const onManageTags = () => {
  console.log('Manage Tags clicked')
}

const updateFilteredTags = (nodeId: number) => {
  if (nodeId) {
    const filteredNodes = inventoryStore?.nodes?.filter((node) => node?.id === nodeId)
    const filteredTags = filteredNodes?.[0]?.tags || []

    tagStore.setFilteredTags(filteredTags)
  } else {
    tagStore.setFilteredTags([])
  }
}

const fetchDataAndInitialize = async (nodeId: any) => {
  await inventoryStore.init()
  if (nodeId) {
    updateFilteredTags(nodeId)
  }
}
onBeforeMount(() => {
  const nodeId = Number(route.params.id)
  nodeStatusStore.setNodeId(nodeId)
  nodeStatusStore.fetchExporters(nodeId)
  fetchDataAndInitialize(nodeId)
})

watchEffect(() => {
  const nodeId = nodeStatusStore?.nodeId
  if (nodeId) {
    updateFilteredTags(nodeId)
  }
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/themes/variables';

.header-wrapper {
  display: flex;
  flex-direction: row;

  .header-button {
    display: flex;
    justify-content: flex-end;
    margin-top: 40px;
    width: 30%;
  }
}

.header {
  margin-top: 40px;
  margin-bottom: 40px;
  display: flex;
  flex-direction: column;
  width: 70%;

  .pre-title {
    @include typography.button;
    color: var(variables.$secondary-variant);
  }

  .page-headline {
    @include typography.headline1;
  }

  .post-title {
    @include typography.caption;
  }
}

.interfaces-spacer {
  height: 2em;
}
</style>

<style lang="scss">
  .tab-panels {
    margin-top: 1em;
  }
</style>
