<template>
  <div
    v-if="isTagManagerOpen"
    class="ctrls"
  >
    <FeatherButton
      v-if="!areAllNodesSelected"
      @click="selectDeselectAllNodes(true)"
      :disabled="!tagStore.isTagEditMode"
      secondary
      data-test="select-all-btn"
    >
      Select all
    </FeatherButton>
    <FeatherButton
      v-else
      @click="selectDeselectAllNodes(false)"
      :disabled="!tagStore.isTagEditMode"
      secondary
      data-test="deselect-all-btn"
    >
      Deselect all
    </FeatherButton>
    <FeatherButton
      @click="openModalSaveTags"
      :disabled="!nodesSelected.length"
      primary
      data-test="open-modal-btn"
    >
      {{ `Save tags to node${nodesSelected.length > 1 ? 's' : ''}` }}
    </FeatherButton>
  </div>
  <ul class="cards">
    <li
      v-for="node in nodes"
      :key="node?.id"
    >
      <section class="header">
        <Icon
          :icon="storageIcon"
          data-test="icon-storage"
        />
        <h4 data-test="heading">{{ node?.label }}</h4>
      </section>
      <section class="content">
        <div>
          <FeatherChipList
            label="List of metric chips"
            data-test="metric-chip-list"
          >
            <MetricChip
              v-for="metric in node?.metrics"
              :key="metric?.type"
              :metric="metric"
            />
          </FeatherChipList>
          <FeatherChipList
            v-if="node.anchor.tagValue.length"
            condensed
            label="Tags"
            class="tag-chip-list"
          >
            <FeatherChip
              v-for="(tag, index) in node.anchor.tagValue"
              :key="index"
            >
              {{ tag.name }}
            </FeatherChip>
          </FeatherChipList>
          <InventoryTextAnchorList
            :anchor="node?.anchor"
            data-test="text-anchor-list"
          />
        </div>
        <InventoryIconActionList
          :node="node"
          class="icon-action"
          data-test="icon-action-list"
        />
      </section>
      <InventoryNodeTagEditOverlay
        v-if="tagStore.isTagEditMode"
        :node="node"
      />
    </li>
  </ul>
  <PrimaryModal
    :visible="isVisible"
    :title="modal.title"
    :class="modal.cssClass"
  >
    <template #content>
      <FeatherChipList
        condensed
        label="Tag list"
      >
        <FeatherChip
          v-for="tag in tagStore.tagsSelected"
          :key="tag.id"
          >{{ tag.name }}</FeatherChip
        >
      </FeatherChipList>
    </template>
    <template #footer>
      <FeatherButton
        data-testid="cancel-btn"
        secondary
        @click="closeModal"
      >
        {{ modal.cancelLabel }}
      </FeatherButton>
      <FeatherButton
        data-testid="save-btn"
        primary
        @click="saveTagsToSelectedNodes"
      >
        {{ modal.saveLabel }}
      </FeatherButton>
    </template>
  </PrimaryModal>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import Storage from '@material-design-icons/svg/outlined/storage.svg'
import { NodeContent } from '@/types/inventory'
import { IIcon } from '@/types'
import { useTagStore } from '@/store/Components/tagStore'
import { useNodeMutations } from '@/store/Mutations/nodeMutations'
import { useInventoryQueries } from '@/store/Queries/inventoryQueries'
import { useInventoryStore } from '@/store/Views/inventoryStore'
import { ModalPrimary } from '@/types/modal'
import useModal from '@/composables/useModal'

// TODO: sort tabContent alpha (default)? to keep list display consistently in same order (e.g. page refresh)
const props = defineProps({
  tabContent: {
    type: Object as PropType<NodeContent[]>,
    required: true
  }
})
const nodes = ref<NodeContent[]>(props.tabContent)

const { openModal, closeModal, isVisible } = useModal()

const tagStore = useTagStore()
const nodeMutations = useNodeMutations()
const inventoryQueries = useInventoryQueries()
const inventoryStore = useInventoryStore()

const isTagManagerOpen = computed(() => inventoryStore.isTagManagerOpen)
const isTagManagerReset = computed(() => inventoryStore.isTagManagerReset)
watch(isTagManagerReset, (isReset) => {
  if (isReset) resetState()
})

const nodesSelected = computed(() => {
  // in case of single node selection toggling
  areAllNodesSelected.value = inventoryStore.nodesSelected.length === nodes.value.length

  return inventoryStore.nodesSelected
})

const areAllNodesSelected = ref(false)
const selectDeselectAllNodes = (areSelected: boolean) => {
  areAllNodesSelected.value = areSelected

  nodes.value.forEach((node) => {
    inventoryStore.addRemoveNodesSelected(node, areSelected)
  })

  nodes.value = nodes.value.map((node) => ({
    ...node,
    isNodeOverlayChecked: areSelected
  }))
}

const openModalSaveTags = () => {
  const selectedNodesLabels = nodesSelected.value.map(({ label }) => label).join(', ')
  const modalContent = areAllNodesSelected.value
    ? 'Add tags to all nodes?'
    : `Add tags to node${inventoryStore.nodesSelected.length > 1 ? '(s)' : ''}: ${selectedNodesLabels}?`
  modal.value = {
    ...modal.value,
    title: modalContent
  }

  openModal()
}

const saveTagsToSelectedNodes = async () => {
  const tags = tagStore.tagsSelected.map(({ name }) => ({ name }))
  const nodeIds = inventoryStore.nodesSelected.map((node) => node.id)
  await nodeMutations.addTagsToNodes({ nodeIds, tags })

  inventoryQueries.fetch()
  resetState()
  closeModal()
}

const resetState = () => {
  tagStore.selectAllTags(false)
  tagStore.setTagEditMode(false)
  inventoryStore.resetSelectedNode()

  nodes.value = nodes.value.map((node) => ({
    ...node,
    isNodeOverlayChecked: false
  }))

  inventoryStore.isTagManagerReset = false
}

const modal = ref<ModalPrimary>({
  title: '',
  cssClass: '',
  content: '',
  id: '',
  cancelLabel: 'cancel',
  saveLabel: 'ok',
  hideTitle: true
})

const storageIcon: IIcon = {
  image: Storage,
  title: 'Node',
  size: 1.5
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins';

.ctrls {
  display: flex;
  justify-content: end;
  padding: var(variables.$spacing-l) 0;
  min-width: vars.$min-width-smallest-screen;
}
ul.cards {
  display: flex;
  flex-flow: row wrap;
  gap: 1%;
  > li {
    position: relative;
    padding: var(variables.$spacing-l) var(variables.$spacing-l);
    border: 1px solid var(variables.$secondary-text-on-surface);
    border-radius: 10px;
    border-left: 10px solid var(variables.$secondary-text-on-surface); // TODO set color dynamically to the node's status
    width: 100%;
    min-width: vars.$min-width-smallest-screen;
    margin-bottom: var(variables.$spacing-m);

    @include mediaQueriesMixins.screen-sm {
      width: 100%;
    }
    @include mediaQueriesMixins.screen-md {
      width: 49%;
      min-width: auto;
    }
    @include mediaQueriesMixins.screen-lg {
      width: 48%;
    }
    @include mediaQueriesMixins.screen-xl {
      width: 32%;
      min-width: 350px;
      max-width: none;
    }
    @include mediaQueriesMixins.screen-xxl {
      width: 24%;
    }

    > .header {
      margin-bottom: var(variables.$spacing-s);
      display: flex;
      flex-direction: row;
      gap: 0.5rem;
      align-items: center;
      > h4 {
        color: var(variables.$primary);
      }
    }
  }

  .content {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    gap: 2rem;
  }
}

.chip-list {
  margin: 0 0 var(variables.$spacing-s);
  gap: 0.5rem;
}
</style>
