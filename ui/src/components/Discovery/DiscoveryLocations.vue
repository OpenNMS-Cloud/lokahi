<template>
    <!-- Search client side -->
    <div class="search-container">
      <FeatherInput
        class="search"
        v-model="searchValue"
        label="Search Locations">
        <template v-slot:pre>
          <FeatherIcon :icon="searchIcon" />
        </template>
      </FeatherInput>
    </div>

    <!-- Locations selection -->
    <FeatherChipList label="Locations" :key="store.selectedLocationIds.toString()">
      <FeatherChip 
        v-for="location in filteredLocations" 
        :key="location.id" 
        class="pointer"
        :class="{ 'selected' : store.selectedLocationIds.includes(location.id) }"
        @click="store.selectLocation(location.id)"
      >
        {{ location.location }}
      </FeatherChip>
    </FeatherChipList>
</template>

<script setup lang="ts">
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
import Search from '@featherds/icon/action/Search'
const discoveryQueries = useDiscoveryQueries()
const store = useDiscoveryStore()
const searchIcon = markRaw(Search)
const searchValue = ref<string>('')

const filteredLocations = computed(() => {
  if (!searchValue.value) return discoveryQueries.locations
  return discoveryQueries.locations.filter(x => x.location?.includes(searchValue.value))
})

onMounted(() => discoveryQueries.getLocations())
</script>

<style scoped lang="scss">
.search-container {
  display: flex;
  justify-content: flex-end;

  .search {
    width: 200px;
  }
}
</style>
