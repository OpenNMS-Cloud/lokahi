<!--
    Autocomplete locations
    props: 
        type: single or multiple
        preLoadedlocations: array of IDs - optional
    emits: 
        location-selected: list of locations
-->
<template>
  <div class="search-location">
    <!-- Search client side -->
    <FeatherAutocomplete
      class="search"
      label="Select a location"
      type="single"
      v-model="searchValue"
      :loading="loading"
      :results="filteredLocations"
      @search="search"
      @update:modelValue="deboncedFn"
    ></FeatherAutocomplete>

    <!-- Locations selection -->
    <FeatherChipList label="Locations">
      <FeatherChip
        v-for="location in selectedLocations"
        :key="location.id"
        class="location-chip"
      >
        <span>{{ location.location }}</span>
        <template v-slot:icon
          ><FeatherIcon
            @click="removeLocation(location)"
            :icon="Icons.Cancel"
        /></template>
      </FeatherChip>
    </FeatherChipList>
  </div>
</template>

<script setup lang="ts">
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import Cancel from '@featherds/icon/navigation/Cancel'
import { markRaw } from 'vue'
import { debounce } from 'lodash'
import { Location } from '@/types/graphql'
import { IAutocompleteItemType } from '@featherds/autocomplete'
import { watchOnce } from '@vueuse/core'
const Icons = markRaw({
  Cancel
})
const emit = defineEmits(['location-selected'])
type TLocationAutocomplete = Location & { _text: string }
const discoveryQueries = useDiscoveryQueries()
const searchValue = ref<Location | undefined>()
const selectedLocations = ref<TLocationAutocomplete[]>([])
const loading = ref(false)
const locations = ref() //locations without selected items
const filteredLocations = ref() //results in autocomplete
const computedLocations = computed(() => discoveryQueries.locations)
const props = defineProps({
  type: {
    type: String,
    required: false,
    default: 'multiple'
  },
  preLoadedlocations: {
    type: Array<string>,
    required: false,
    default: []
  }
})

onMounted(() => discoveryQueries.getLocations())

const initLocations = () => {
  filteredLocations.value = computedLocations.value as Location[]
  locations.value = computedLocations.value as Location[]
  if (props.preLoadedlocations.length) {
    selectedLocations.value = locations.value.filter((l: Location) => props.preLoadedlocations.includes(l.id))
    locations.value = locations.value.filter((l: Location) => !props.preLoadedlocations.includes(l.id))
    filteredLocations.value = locations.value
  } else {
    if (computedLocations.value.length == 1) {
      selectedLocations.value = computedLocations.value as TLocationAutocomplete[]
      locations.value = []
      filteredLocations.value = []
      emit('location-selected', selectedLocations.value)
    }
  }
}
watchOnce(computedLocations, () => {
  if (computedLocations) {
    initLocations()
  }
})

watch(props, () => {
  initLocations()
})

const search = (q: string) => {
  if (!q) {
    searchValue.value = undefined
    return []
  }
  loading.value = true
  const query = q.toLowerCase()
  filteredLocations.value = locations.value
    .filter((x: any) => x.location?.toLowerCase().indexOf(query) > -1)
    .map((x: any) => ({
      _text: x?.location,
      id: x?.id
    }))
  loading.value = false
}

//using debounce temporary because of bug in feather/autocomplete
const deboncedFn = debounce(
  (selected: IAutocompleteItemType | IAutocompleteItemType[] | undefined) => {
    if (selected) {
      const selectedLocation = selected as TLocationAutocomplete
      selectedLocation.location = selectedLocation._text
      if (props.type === 'single') {
        selectedLocations.value = [selectedLocation]
      } else {
        const exists = selectedLocations.value.find((l) => l.id == selectedLocation.id)
        if (!exists) {
          selectedLocations.value.push(selectedLocation)
        }
      }
      locations.value = locations.value.filter((l: Location) => l.id !== selectedLocation.id)
      searchValue.value = undefined
      emit('location-selected', selectedLocations.value)
    }
  },
  200,
  {
    leading: false,
    trailing: true
  }
)

const removeLocation = (location: Location) => {
  if (location?.id) {
    selectedLocations.value = selectedLocations.value.filter((l: Location) => l.id !== location.id)
    locations.value.push(location)
    emit('location-selected', selectedLocations.value)
  }
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@/styles/mediaQueriesMixins';

.search-location {
  display: flex;
  flex-direction: column;

  .search {
    width: 100%;
  }
  .location-chip {
    cursor: pointer;
  }
}
:deep(.feather-input-sub-text) {
  display: none !important;
}
:deep(.chip-label-button) {
  display: flex;
  .chip-icon {
    order: 2;
    &:hover {
      cursor: pointer;
    }
    > svg {
      &:hover {
        cursor: pointer;
      }
    }
  }
  > .label {
    order: 1;
  }
}
</style>
