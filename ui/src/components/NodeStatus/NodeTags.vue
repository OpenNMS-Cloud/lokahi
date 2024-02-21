<template>
  <div class="node-tag-section">
    <section class="feather-row">
      <h3 data-test="heading" class="feather-col-6">Tags</h3>
      <FeatherButton text class="feather-col-6 btn" @click="onManageTags">Manage</FeatherButton>
    </section>
    <section class="node-component-content">
      <FeatherChipList
      label=""
      data-test="node-tags-chip-list"
      >
        <FeatherChip
        v-for="(tag) in tagStore?.filteredTags"
        :key="tag?.id"
        >
          <span class="node-tag-content">{{ tag?.name }}</span>
          <FeatherIcon
          @click="removeTag(tag)"
          :icon="CancelIcon"
          class="pointer"
          />
        </FeatherChip>
    </FeatherChipList>
  </section>
  </div>
</template>

<script lang="ts" setup>
import CancelIcon from '@featherds/icon/navigation/Cancel'
import { useNodeMutations } from '@/store/Mutations/nodeMutations'
import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import { useTagStore } from '@/store/Components/tagStore'
// import { InventoryItem } from '@/types'
// import { PropType } from 'vue'
import useSnackbar from '@/composables/useSnackbar'

// const props = defineProps({
//     type: Object as PropType<InventoryItem['tags']>,
//     required: true
//   }
// })

const nodeMutations = useNodeMutations()
const nodeStatusStore = useNodeStatusStore()
const { showSnackbar } = useSnackbar()
const tagStore = useTagStore()

// watchEffect(() => {
//   if (props?.nodeTagsContent) {
//     tagStore.setFilteredTags(props.nodeTagsContent)
//   }
// })

const onManageTags = () => {
  console.log('Manage Tags clicked in NodeTags')
}

const removeTag = async (tag: any) => {
  if (tag?.id) {
    const removeTagResult = await nodeMutations.removeTagsFromNodes({ nodeIds: nodeStatusStore.nodeId, tagIds: tag?.id })

    if (!removeTagResult.error) {
      tagStore.filterTag(tag)

      showSnackbar({
        msg: 'Tag successfully removed from node.'
      })
    } else {
      showSnackbar({
        msg: 'Error removing tag.'
      })
    }
  }
}
</script>

  <style lang="scss" scoped>
  @use '@featherds/styles/themes/variables';
  @use '@/styles/vars';
  @use '@/styles/mediaQueriesMixins';
  @use '@featherds/styles/mixins/typography';

  .node-tag-section {
    margin: 0px 1.5rem 1.5rem 1.5rem;
    .feather-row {
      margin-bottom: var(variables.$spacing-s);
      display: flex;
      gap: 0.5rem;
      align-items: center;
      justify-content: space-between;
      .btn-text {
        color: var(variables.$primary);
        font-weight: bold;
        &:hover, &:focus {
          border-color: transparent;
        }
      }
  }
}
  .node-component-content {
    .node-tag-content {
      padding-right: var(variables.$spacing-s);
    }
    :deep(.chip-icon) {
      background-color: transparent;
      padding: 6px 8px 6px 12px;
    }
}
  </style>

