<template>
  <div class="search-location">
    <!-- Search client side -->
    <FeatherAutocomplete
      class="search"
      label="Locations"
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
        {{ location.location }}
        <FeatherIcon
          :icon="Icons.Cancel"
          class="icon"
          @click="removeLocation(location)"
        />
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
  }
})

onMounted(() => discoveryQueries.getLocations())
watchEffect(() => {
  if (computedLocations) {
    filteredLocations.value = computedLocations.value as Location[]
    locations.value = computedLocations.value as Location[]
  }
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
        selectedLocations.value.push(selectedLocation)
      }
      locations.value = locations.value.filter((l: Location) => l.id !== selectedLocation.id)
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

.search-location {
  display: flex;
  align-items: baseline;
  .search {
    width: 300px;
    margin-right: var(variables.$spacing-m);
  }
  .location-chip {
    margin-bottom: var(variables.$spacing-s);
    cursor: pointer;
  }
}
</style>
