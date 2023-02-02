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
      @update:modelValue="setLocation"
    ></FeatherAutocomplete>

    <!-- Locations selection -->
    <FeatherChipList label="Locations">
      <FeatherChip
        v-for="location in selectedLocations"
        :key="location.id"
        class="pointer"
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
const Icons = markRaw({
  Cancel
})
const emit = defineEmits(['location-selected'])

const discoveryQueries = useDiscoveryQueries()
const searchValue = ref<string>(undefined)
const selectedLocations = ref([])
const loading = ref(false)
const locations = ref()
const filteredLocations = ref()
//const filteredLocations = computed(() => discoveryQueries.locations)
locations.value = [
  {
    id: 1,
    location: 'Default',
    tenantId: 'opennms-prime'
  },
  {
    id: 2,
    location: 'Montreal',
    tenantId: 'opennms-prime'
  },
  {
    id: 3,
    location: 'Ottawa',
    tenantId: 'opennms-prime'
  },
  {
    id: 4,
    location: 'Toronto',
    tenantId: 'opennms-prime'
  }
]
onMounted(() => discoveryQueries.getLocations())

const search = (q: string) => {
  if (!q) {
    searchValue.value = undefined
    return []
  }
  loading.value = true
  const query = q.toLowerCase()
  filteredLocations.value = locations.value
    .filter((x: any) => x.location.toLowerCase().indexOf(query) > -1)
    .map((x) => ({
      _text: x.location,
      id: x.id
    }))
  loading.value = false
}

const setLocation = (selected) => {
  if (selected) {
    selectedLocations.value.push({
      id: selected.id,
      location: selected._text
    })
    locations.value = locations.value.filter((l) => l.id !== selected.id)
    emit('location-selected', selectedLocations)
  }
}

const removeLocation = (location) => {
  if (location?.id) {
    selectedLocations.value = selectedLocations.value.filter((l) => l.id !== location.id)
    locations.value.push(location)
    emit('location-selected', selectedLocations)
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
}
</style>
