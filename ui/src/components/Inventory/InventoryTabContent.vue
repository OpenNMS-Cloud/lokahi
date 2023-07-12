<template>
  <ul class="cards">
    <li v-for="node in tabContent" :key="node?.id">
      <section class="header">
        <Icon :icon="storageIcon" data-test="icon-storage" />
        <h4 data-test="heading">{{ node?.label }}</h4>
      </section>
      <section class="content">
        <div>
          <FeatherChipList label="List of metric chips" data-test="metric-chip-list" v-if="isMonitored(node)">
            <MetricChip v-for="metric in node?.metrics" :key="metric?.type" :metric="metric" />
          </FeatherChipList>

          <div class="tags-count-box" @click="openModalForDeletingTags(node)">
            Tags: <span class="count">{{ node.anchor.tagValue.length }}</span>
          </div>

          <InventoryTextAnchorList :anchor="node?.anchor" data-test="text-anchor-list" />
        </div>
        <InventoryIconActionList :node="node" class="icon-action" data-test="icon-action-list" />
      </section>
      <InventoryNodeTagEditOverlay v-if="tagStore.isTagEditMode" :node="node" />
    </li>
  </ul>
  <PrimaryModal :visible="isVisible" :title="modal.title" :class="modal.cssClass">
    <template #content>
      <FeatherChipList :key="tagsForDeletion.toString()" condensed label="Tags">
        <FeatherChip v-for="(tag, index) in availableTagsToDelete" :key="index" @click="selectTagForDeletion(tag)"
          :class="{ selected: tagsForDeletion.some((t) => t.id === tag.id) }" class="pointer">
          {{ tag.name }}
        </FeatherChip>
      </FeatherChipList>
    </template>
    <template #footer>
      <FeatherButton secondary @click="closeModal">
        {{ modal.cancelLabel }}
      </FeatherButton>
      <FeatherButton primary @click="removeTagsFromNodes" :disabled="!tagsForDeletion.length">
        {{ modal.saveLabel }}
      </FeatherButton>
    </template>
  </PrimaryModal>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import Storage from '@material-design-icons/svg/outlined/storage.svg'
import { IIcon, InventoryNode } from '@/types'
import { useTagStore } from '@/store/Components/tagStore'
import { useNodeMutations } from '@/store/Mutations/nodeMutations'
import { useInventoryQueries } from '@/store/Queries/inventoryQueries'
import { useInventoryStore } from '@/store/Views/inventoryStore'
import { ModalPrimary } from '@/types/modal'
import useModal from '@/composables/useModal'
import { Tag, TagListNodesRemoveInput } from '@/types/graphql'
import { isMonitored } from './inventory.utils'

// TODO: sort tabContent alpha (default)? to keep list display consistently in same order (e.g. page refresh)
const props = defineProps({
  tabContent: {
    type: Object as PropType<InventoryNode[]>,
    required: true
  },
  state: {
    type: String,
    required: true
  }
})

const availableTagsToDelete = ref(<Tag[]>[])
const tagsForDeletion = ref([] as Tag[])
const nodeIdForDeletingTags = ref()

const { openModal, closeModal, isVisible } = useModal()

const tagStore = useTagStore()
const nodeMutations = useNodeMutations()
const inventoryQueries = useInventoryQueries()
const inventoryStore = useInventoryStore()

const isTagManagerReset = computed(() => inventoryStore.isTagManagerReset)
watch(isTagManagerReset, (isReset) => {
  if (isReset) resetState()
})



const removeTagsFromNodes = async () => {
  const payload: TagListNodesRemoveInput = {
    nodeIds: [nodeIdForDeletingTags.value], // designs only account for 1 node at a time
    tagIds: tagsForDeletion.value.map((tag) => tag.id)
  }

  await nodeMutations.removeTagsFromNodes(payload)

  await inventoryQueries.fetchByState(props.state)
  resetState()
  closeModal()
}

const selectTagForDeletion = (tag: Tag) => {
  const isTagAlreadySelected = tagsForDeletion.value.some(({ name }) => name === tag.name)

  if (isTagAlreadySelected) {
    tagsForDeletion.value = tagsForDeletion.value.filter(({ name }) => name !== tag.name)
  } else {
    tagsForDeletion.value.push(tag)
  }
}

const resetState = () => {
  tagStore.selectAllTags(false)
  tagStore.setTagEditMode(false)
  inventoryStore.resetSelectedNode()
  tagsForDeletion.value = []
  inventoryStore.isTagManagerReset = false
}

const openModalForDeletingTags = (node: InventoryNode) => {
  if (!node.anchor.tagValue.length) return

  nodeIdForDeletingTags.value = node.id
  availableTagsToDelete.value = node.anchor.tagValue

  modal.value.title = node.label!
  modal.value.saveLabel = 'Delete'

  openModal()
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
@use '@featherds/styles/mixins/typography';

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

  >li {
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

    >.header {
      margin-bottom: var(variables.$spacing-s);
      display: flex;
      flex-direction: row;
      gap: 0.5rem;
      align-items: center;

      >h4 {
        color: var(variables.$primary);
      }
    }
  }

  .content {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    gap: 2rem;

    .tags-count-box {
      background: var(variables.$secondary-variant);
      color: var(variables.$primary-text-on-color);
      padding: var(variables.$spacing-xxs);
      border-radius: vars.$border-radius-s;
      width: 75px;
      text-align: center;
      cursor: pointer;

      .count {
        @include typography.subtitle2;
        color: var(variables.$primary-text-on-color);
      }
    }
  }
}

.chip-list {
  margin: 0 0 var(variables.$spacing-s);
  gap: 0.5rem;
}
</style>
