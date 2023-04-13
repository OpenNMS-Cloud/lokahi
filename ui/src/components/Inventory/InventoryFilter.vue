<template>
  <ul class="filter-container">
    <li class="autocomplete">
      <FeatherInput
        @update:model-value="searchNodesByLabel"
        label="Search labels"
        data-test="search-by-label"
        ref="searchNodesByLabelRef"
      >
        <template v-slot:post>
          <FeatherIcon :icon="icons.Search" />
        </template>
      </FeatherInput>
    </li>
    <li>
      <div class="or">OR</div> 
    </li>
    <li class="autocomplete">
      <BasicAutocomplete
        @items-selected="searchNodesByTags"
        :get-items="tagQueries.getTagsSearch"
        :items="tagQueries.tagsSearched"
        label="Search tags"
        :allow-new="false"
        render-type="multi"
        data-test="search-by-tags"
        ref="searchNodesByTagsRef"
      />
    </li>
    <li>
      <InventoryTagManagerCtrl data-test="tag-manager-ctrl" />
    </li>
  </ul>
  <InventoryTagManager v-if="isTagManagerOpen" />
</template>

<script lang="ts" setup>
import Search from '@featherds/icon/action/Search'
import { fncArgVoid } from '@/types'
import { useInventoryStore } from '@/store/Views/inventoryStore'
import { useInventoryQueries } from '@/store/Queries/inventoryQueries'
import { useTagQueries } from '@/store/Queries/tagQueries'
import { Tag } from '@/types/graphql'

const inventoryStore = useInventoryStore()
const inventoryQueries = useInventoryQueries()
const tagQueries = useTagQueries()

const searchNodesByLabelRef = ref()
const searchNodesByTagsRef = ref()

const icons = markRaw({
  Search
})

const isTagManagerOpen = computed(() => inventoryStore.isTagManagerOpen)

// Current BE setup only allows search by names OR tags.
// so we clear the other search to avoid confusion
const searchNodesByLabel: fncArgVoid = useDebounceFn((val: string | undefined) => {
  // clear tags search
  searchNodesByTagsRef.value.reset()

  if (val === undefined) return
  inventoryQueries.getNodesByLabel(val)
})

const searchNodesByTags: fncArgVoid = (tags: Tag[]) => {
  // clear label search
  searchNodesByLabelRef.value.internalValue = undefined

  // if empty tags array, call regular fetch
  if (!tags.length) {
    inventoryQueries.fetch()
    return
  }
  const tagNames = tags.map((tag) => tag.name!)
  inventoryQueries.getNodesByTags(tagNames)
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/btns.scss';

.filter-container {
  margin: var(variables.$spacing-l) 0;
  display: flex;
  flex-flow: row wrap;
  > * {
    margin-right: var(variables.$spacing-l);
  }
  > .autocomplete {
    min-width: 13rem;
  }
  .or {
    line-height: 2.6;
  }
}
</style>
