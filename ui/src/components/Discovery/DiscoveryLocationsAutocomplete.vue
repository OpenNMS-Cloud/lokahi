<!--
    Autocomplete locations
    props: 
        preLoadedlocation: location name
    emits: 
        location-selected: location name
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
      :disabled="disabled"
    ></FeatherAutocomplete>

    <!-- Locations selection -->
    <FeatherChipList
      v-if="selectedLocation && selectedLocation.location"
      label="Locations"
    >
      <FeatherChip class="location-chip">
        <span>{{ selectedLocation.location }}</span>
        <template v-slot:icon
          ><FeatherIcon
            v-if="!disabled"
            @click="removeLocation"
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
import { debounce, first } from 'lodash'
import { MonitoringLocation } from '@/types/graphql'
import { IAutocompleteItemType } from '@featherds/autocomplete'
import { watchOnce } from '@vueuse/core'
import { object } from 'yup'

const Icons = markRaw({
  Cancel
})
const emit = defineEmits(['location-selected'])
type TLocationAutocomplete = MonitoringLocation & { _text?: string }
const discoveryQueries = useDiscoveryQueries()
const searchValue = ref<MonitoringLocation | undefined>()
const selectedLocation = ref<TLocationAutocomplete | null>(null)
const loading = ref(false)
const locations = ref() //locations without selected items
const filteredLocations = ref() //results in autocomplete
const inputRef = ref() // actual autocomplete input
const errMsgDisplay = ref('none') // for sub text css display
const computedLocations = computed(() => discoveryQueries.locations)

const props = defineProps<{
  preLoadedlocation?: string,
  disabled?: boolean
}>()

onMounted(() => discoveryQueries.getLocations())

const initLocations = () => {
  filteredLocations.value = computedLocations.value as MonitoringLocation[]
  locations.value = computedLocations.value as MonitoringLocation[]
  selectedLocation.value = null
  if (props.preLoadedlocation) {
    selectedLocation.value = locations.value.filter((l: MonitoringLocation) => l.id == props.preLoadedlocation)[0]
    locations.value = locations.value.filter((l: MonitoringLocation) => l.id !== props.preLoadedlocation)
    filteredLocations.value = locations.value
  }
}
watchOnce(computedLocations, () => {
  initLocations()
  if (computedLocations.value.length == 1) {
    selectedLocation.value = first(computedLocations.value) as MonitoringLocation
    locations.value = []
    filteredLocations.value = []
    emit('location-selected', selectedLocation.value)
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
  (item: IAutocompleteItemType | IAutocompleteItemType[] | undefined) => {
    const selected = item as IAutocompleteItemType
    if (selected && selected._text) {
      selectedLocation.value = selected as TLocationAutocomplete
      locations.value = computedLocations.value.filter((l: MonitoringLocation) => l.id !== selectedLocation.value?.id)
      searchValue.value = undefined
      inputRef.value?.handleOutsideClick()
      emit('location-selected', selectedLocation.value)
      handleErrDisplay()
    }
  },
  200,
  {
    leading: false,
    trailing: true
  }
)

const removeLocation = () => {
  if (selectedLocation.value) {
    locations.value.push(selectedLocation.value)
    selectedLocation.value = null
    searchValue.value = undefined
    handleErrDisplay()
  }
}

const locationErrMsg = 'Location is required.'
const locationV = object().test({
  name: 'has-location',
  test: () => Boolean(selectedLocation.value),
  message: locationErrMsg
})
const handleErrDisplay = () => {
  if (inputRef.value) {
    inputRef.value.handleInputBlur() // runs yup validate

    nextTick(() => {
      // add/remove the feather input subtext display
      errMsgDisplay.value =
        document.getElementById(inputRef.value.subTextId)?.children[0].innerHTML === locationErrMsg ? 'flex' : 'none'
    })
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
