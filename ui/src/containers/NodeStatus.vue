<template>
  <div class="full-page-container">
    <div class="header">
      <div class="pre-title">Network Inventory</div>
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
    <NodeInfoTable />
    <SNMPInterfacesTable v-if="!nodeStatusStore.isAzure" />
    <IPInterfacesTable />
    <EventsTable />
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
import useModal from '@/composables/useModal'
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import EditIcon from '@featherds/icon/action/EditMode'

const nodeStatusStore = useNodeStatusStore()
const queries = useNodeStatusQueries()
const route = useRoute()
const { openModal, closeModal, isVisible } = useModal()

onBeforeMount(() => {
  const nodeId = Number(route.params.id)
  nodeStatusStore.setNodeId(nodeId)
  nodeStatusStore.fetchExporters(nodeId)
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/themes/variables';

.header {
  margin-top: 40px;
  margin-bottom: 40px;
  display: flex;
  flex-direction: column;

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
</style>
