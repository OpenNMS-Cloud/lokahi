<template>
  <ul class="filter-container">
    <li v-if="!onlyTags" class="autocomplete flex margin-bottom">
      <FeatherSelect class="filter-type-selector" label="Search Type" textProp="name"
        :options="[{ id: 1, name: 'Labels' }, { id: 2, name: 'Tags' }]" :modelValue="inventoryStore.searchType"
        @update:modelValue="inventoryStore.setSearchType">
      </FeatherSelect>
      <FeatherAutocomplete v-if="inventoryStore.searchType.name === 'Tags'" type="multi"
        :modelValue="inventoryStore.tagsSelected" @update:modelValue="inventoryStore.addSelectedTag"
        :results="tagQueries.tagsSearched" @search="tagQueries.getTagsSearch" class="inventory-auto" label="Search"
        :allow-new="false" textProp="name" render-type="multi" data-test="search-by-tags" ref="searchNodesByTagsRef" />
      <FeatherInput v-if="inventoryStore.searchType.name === 'Labels'" @update:model-value="searchNodesByLabel"
        label="Search" class="inventory-search" data-test="search" ref="searchNodesByLabelRef">
      </FeatherInput>
    </li>
    <li class="push-right">

      <InventoryTagManagerCtrl class="tag-manager" data-test="tag-manager-ctrl" />
    </li>

  </ul>
  <div :class="[inventoryStore.isTagManagerOpen ? 'padding' : 'no-padding']">
    <InventoryTagManager :visible="inventoryStore.isTagManagerOpen" />
  </div>
  <div class="margin-bottom">
    <FeatherButton text v-if="tagStore.isTagEditMode" data-test="select-all" @click="inventoryStore.selectAll(nodes)">
      Select All
    </FeatherButton>
    <FeatherButton data-test="clear-selection" text
      v-if="inventoryStore.nodesSelected.length > 0 && inventoryStore.isTagManagerOpen" @click="inventoryStore.clearAll">
      Clear Selection
    </FeatherButton>
  </div>
</template>

<script lang="ts" setup>
import Search from '@featherds/icon/action/Search'
import { InventoryNode, fncArgVoid } from '@/types'
import { useInventoryStore } from '@/store/Views/inventoryStore'
import { useInventoryQueries } from '@/store/Queries/inventoryQueries'
import { useTagQueries } from '@/store/Queries/tagQueries'
import { Tag } from '@/types/graphql'
import { FeatherDropdown, FeatherDropdownItem } from '@featherds/dropdown'
import MenuIcon from "@featherds/icon/navigation/MoreHoriz";
import { PropType } from 'vue'
import { FeatherButton } from '@featherds/button'
import { useTagStore } from '@/store/Components/tagStore'
const inventoryStore = useInventoryStore()
const tagStore = useTagStore();
const inventoryQueries = useInventoryQueries()
const tagQueries = useTagQueries()

defineProps({
  onlyTags: {
    type: Boolean,
    default: false
  },
  state: {
    type: String,
    required: true
  },
  nodes: {
    type: Object as PropType<InventoryNode[]>,
    required: true
  },
})

const searchNodesByLabelRef = ref()

const icons = markRaw({
  Search
})

// Current BE setup only allows search by names OR tags.
// so we clear the other search to avoid confusion
const searchNodesByLabel: fncArgVoid = useDebounceFn((val: string | undefined) => {

  if (val === undefined) return
  inventoryQueries.getNodesByLabel(val)
})

const searchNodesByTags: fncArgVoid = (tags: Tag[]) => {
  // clear label search
  searchNodesByLabelRef.value.internalValue = undefined

  // if empty tags array, call regular fetch
  if (!tags.length) {
    inventoryQueries.getMonitoredNodes()
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
  margin-bottom: 0;
  align-items: center;
  min-height: 55px;

  >* {
    margin-right: var(variables.$spacing-l);
  }

  >.autocomplete {
    min-width: 13rem;
  }

  .or {
    line-height: 2.6;
  }

  :deep(.feather-input-sub-text) {
    display: none;
  }
}

.push-right {
  margin-left: auto;
  margin-right: 0;
}

.flex {
  display: flex;
  align-items: center;
}

.inventory-search {
  min-width: 240px;
  margin-right: 24px;
}

.inventory-auto {
  min-width: 400px;

  :deep(.feather-autocomplete-input) {
    min-width: 100px;
  }

  :deep(.feather-autocomplete-content) {
    display: block;
  }
}

.margin-bottom {
  margin-bottom: var(variables.$spacing-s);
}

.filter-type-selector {
  margin-right: 12px;
  max-width: 120px;
}
</style>
