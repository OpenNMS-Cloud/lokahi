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
      :schema="locationV"
      ref="inputRef"
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
import { object } from 'yup'

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
const inputRef = ref() // actual autocomplete input
const errMsgDisplay = ref('none') // for sub text css display
const computedLocations = computed(() => discoveryQueries.locations)

const props = defineProps({
  type: {
    type: String,
    required: false
  },
  preLoadedlocations: {
    type: Array<string | undefined>,
    required: false,
    default: []
  }
})

onMounted(() => discoveryQueries.getLocations())

const initLocations = () => {
  filteredLocations.value = computedLocations.value as Location[]
  locations.value = computedLocations.value as Location[]
  selectedLocations.value = []
  if (props.preLoadedlocations.length && props.preLoadedlocations[0]) {
    selectedLocations.value = locations.value.filter(
      (l: Location) => l.location && props.preLoadedlocations.includes(l.location)
    )
    locations.value = locations.value.filter(
      (l: Location) => l.location && !props.preLoadedlocations.includes(l.location)
    )
    filteredLocations.value = locations.value
  }
}
watchOnce(computedLocations, () => {
  initLocations()
  if (computedLocations.value.length == 1) {
    selectedLocations.value = computedLocations.value as TLocationAutocomplete[]
    locations.value = []
    filteredLocations.value = []
    emit('location-selected', selectedLocations.value)
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
      location: x?.location,
      id: x?.id
    }))
  loading.value = false
}

//using debounce temporary because of bug in feather/autocomplete
const deboncedFn = debounce(
  (selected: IAutocompleteItemType | IAutocompleteItemType[] | undefined) => {
    if (selected) {
      const selectedLocation = selected as TLocationAutocomplete
      if (props.type === 'single') {
        locations.value = computedLocations.value
        selectedLocations.value = [selectedLocation]
      } else {
        const exists = selectedLocations.value.find((l) => l.id == selectedLocation.id)
        if (!exists) {
          selectedLocations.value.push(selectedLocation)
        }
      }
      locations.value = locations.value.filter((l: Location) => l.id !== selectedLocation.id)
      searchValue.value = undefined
      inputRef.value?.handleOutsideClick()
      emit('location-selected', selectedLocations.value)
      handleErrDisplay()
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
    handleErrDisplay()
  }
}

const locationErrMsg = 'Location is required.'
const locationV = object().test({
  name: 'has-location',
  test: () => Boolean(selectedLocations.value.length),
  message: locationErrMsg
})
const handleErrDisplay = () => {
  inputRef.value.handleInputBlur() // runs yup validate

  nextTick(() => {
    // add/remove the feather input subtext display
    errMsgDisplay.value =
      document.getElementById(inputRef.value.subTextId)?.children[0].innerHTML === locationErrMsg ? 'flex' : 'none'
  })
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
  display: v-bind(errMsgDisplay) !important;
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
:deep(.post) {
  &:last-child {
    display: none !important;
  }
}
</style>
