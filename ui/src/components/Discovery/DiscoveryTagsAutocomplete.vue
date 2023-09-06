<template>
  <AtomicAutocomplete
    :loading="discoveryQueries.isTagsSearchFetching"
    :outsideClicked="closeAutocomplete"
    :itemClicked="itemClicked"
    :resultsVisible="isAutoCompleteOpen"
    :focusLost="onFocusLost"
    :wrapperClicked="wrapperClicked"
    :results="discoveryQueries.tagsSearched.map((d) => d.name)"
    :inputValue="inputValue"
    inputLabel="Search/Add tags (optional)"
    :textChanged="textChanged"
    :errMsg="errorMessage"
    :allowNew="true"
  />
  <FeatherChipList label="">
    <FeatherChip
      v-for="tag in tags"
      :key="tag.name"
    >
      <template v-slot:icon>
        <FeatherIcon
          class="pointer"
          @click="() => removeTag(tag)"
          :icon="CancelIcon"
        />
      </template>
      {{ tag.name }}
    </FeatherChip>
  </FeatherChipList>
</template>
<script setup lang="ts">
import useAtomicAutocomplete from '@/composables/useAtomicAutocomplete'
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { Tag, TagCreateInput } from '@/types/graphql'
import CancelIcon from '@featherds/icon/navigation/Cancel'
import { PropType } from 'vue'
const discoveryQueries = useDiscoveryQueries()
const errorMessage = ref()
const props = defineProps({
  tags: { type: Array as PropType<Array<TagCreateInput> | undefined> },
  updateTags: { type: Function as PropType<(newTags: Array<TagCreateInput>) => void>, default: () => ({}) }
})

const addTag = (tag: TagCreateInput) => {
  const newTags = [...(props.tags || [])]
  newTags.push(tag)
  props.updateTags(newTags)
}
const removeTag = (tag: TagCreateInput) => {
  props.updateTags(props.tags?.filter((d) => d.name !== tag.name) || [])
}

const { closeAutocomplete, itemClicked, isAutoCompleteOpen, onFocusLost, wrapperClicked, textChanged, inputValue } =
  useAtomicAutocomplete(discoveryQueries.getTagsSearch, () => discoveryQueries.tagsSearched.length, addTag)
</script>
<style lang="scss" scoped>
.pointer {
  cursor: pointer;
}
</style>
