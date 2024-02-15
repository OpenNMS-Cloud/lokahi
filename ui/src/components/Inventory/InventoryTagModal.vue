<template>
  <PrimaryModal :visible="visible" :title="title" :class="[modal.cssClass, 'inventory-tag-modal']">
    <template #content>
      <h3>Tags</h3>
      <h4 class="subhead-1">{{ node.nodeLabel }}</h4>
      <h4 class="subhead-1 existing-tags">Existing Tags</h4>
      <FeatherChipList label="Tags">
        <FeatherChip v-for="(tag) in tagStore.filteredTags" :key="tag.id"
          :class="{ selected: tagsForDeletion.some((t) => t.id === tag.id) }">
          {{ tag.name }}
          <template v-slot:icon>
            <FeatherIcon :icon="deleteIcon" @click="tagStore.filterTag(tag)" class="pointer" />
          </template>
        </FeatherChip>
      </FeatherChipList>
      <h4 class="subhead-1 add-tags">Add Tags</h4>
          <AtomicAutocomplete class="tag-modal-complete" data-test="search-by-tags-inner" inputLabel="Search or create new tag"
              :loading="tagQueries.tagsSearchIsFetching" :outsideClicked="closeAutocomplete" :itemClicked="itemClicked"
              :resultsVisible="isAutoCompleteOpen" :focusLost="onFocusLost" :wrapperClicked="wrapperClicked"
              :results="tagQueries.tagsSearched.filter((t) => { return !tagStore.filteredTags.find((d) => d.name === t.name) }).map((d) => d.name)" :inputValue="inputValue" :textChanged="textChanged" />
    </template>
    <template #footer>
      <FeatherButton secondary @click="closeModal">
        {{ modal.cancelLabel }}
      </FeatherButton>
      <FeatherButton primary @click="tagStore.saveFilteredTagsToNode">
        {{ modal.saveLabel }}
      </FeatherButton>
    </template>
  </PrimaryModal>
</template>

<script setup lang="ts">
import { Tag } from '@/types/graphql'
import { InventoryItem } from '@/types/inventory'
import { ModalPrimary } from '@/types/modal'
import { PropType } from 'vue'
import deleteIcon from '@featherds/icon/navigation/Cancel'
import useAtomicAutocomplete from '@/composables/useAtomicAutocomplete'
import { useTagQueries } from '@/store/Queries/tagQueries'
import { useTagStore } from '@/store/Components/tagStore'

const props = defineProps({
  closeModal: { type: Function as PropType<(payload: any[]) => void>, default: () => null, required: true },
  node: { type: Object as PropType<InventoryItem>, default: () => ({}), required: true },
  title: { type: String, default: '', required: true },
  visible: { type: Boolean, default: false, required: true }
})

const tagQueries = useTagQueries()
const tagsForDeletion = ref([] as Tag[])
const nodeIdForDeletingTags = ref()
const tagStore = useTagStore()

const {
  isAutoCompleteOpen,
  closeAutocomplete,
  wrapperClicked,
  onFocusLost,
  itemClicked,
  textChanged,
  inputValue
} =
  useAtomicAutocomplete(tagQueries.getTagsSearch, () => tagQueries.tagsSearched.length, tagStore.addFilteredTag)

const modal = ref<ModalPrimary>({
  title: '',
  cssClass: '',
  content: '',
  id: '',
  cancelLabel: 'cancel',
  saveLabel: 'Apply',
  hideTitle: true
})

watchEffect(() => {
  if (props.visible && props.node && props.node.tags) {
    modal.value.saveLabel = 'Apply'
    nodeIdForDeletingTags.value = props.node.id
    tagStore.setFilteredTags(props.node.tags)
    modal.value.title = props.node.nodeLabel || ''
  } else if (!props.visible) {
    inputValue.value = ''
  }
})


</script>
<style lang="scss">

.inventory-tag-modal .dialog-body header {
  margin-bottom: 0;
}

.inventory-tag-modal .content {
  max-width: 490px;
}

</style>

<style lang="scss" scoped>
.inventory-tag-modal h3 {
  margin-bottom: var(--feather-spacing-m);
}

.existing-tags {
  margin-top: var(--feather-spacing-m);
}
.add-tags {
  margin-top: var(--feather-spacing-l);
  margin-bottom: var(--feather-spacing-s);
}
</style>

