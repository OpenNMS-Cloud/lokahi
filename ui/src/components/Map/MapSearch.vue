<template>
  <FeatherAutocomplete
    v-model="searchStr"
    type="multi"
    :results="results"
    label="Search"
    @search="resetLabelsAndSearch"
    :loading="loading"
    text-prop="label"
    @update:modelValue="selectItem"
    :labels="labels"
  ></FeatherAutocomplete>
</template>

<script
  setup
  lang="ts"
>
import { debounce } from 'lodash'
import { useMapStore } from '@/store/Views/mapStore'
import { useSearchStore } from '@/store/Views/searchStore'
import { FeatherAutocomplete } from '@featherds/autocomplete'

const emit = defineEmits(['fly-to-node', 'set-bounding-box'])

const mapStore = useMapStore()
const searchStore = useSearchStore()
const searchStr = ref()
const loading = ref(false)
const defaultLabels = { noResults: 'Searching...' }
const labels = ref(defaultLabels)

const selectItem: any = (items: { label: string }[]) => {
  const nodeLabels = items.map((item) => item.label)
  mapStore.searchedDeviceLabels = nodeLabels
  if (nodeLabels.length) {
    if (nodeLabels.length === 1) {
      // fly to last selected node
      emit('fly-to-node', nodeLabels[0])
    } else {
      // set bounding box for all searched nodes
      emit('set-bounding-box', nodeLabels)
    }
  }
}

const resetLabelsAndSearch = (value: string) => {
  labels.value = defaultLabels
  search(value)
}

const search = debounce(async (value: string) => {
  if (!value) return
  loading.value = true
  await searchStore.search(value)
  labels.value = { noResults: 'No results found' }
  loading.value = false
}, 1000)

const results = computed(() => {
  if (searchStore.searchResults[0]) {
    return searchStore.searchResults[0].results
  }
  return []
})
</script>

<style
  lang="scss"
  scoped
>
@import "@featherds/styles/themes/variables";

.map-search {
  z-index: 1000;
  width: 290px !important;
  :deep(.feather-input-border) {
    background: var($surface);
  }
}
</style>

