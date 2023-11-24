<template>
  <div
    class="locations-card"
    :class="{ selected: selectedCard }"
  >
    <div
      class="locations-card-items"
      @click="getMinionsForLocationId(location.id)"
    >
      <div class="name">
        <ButtonTextIcon
          :item="nameBtn"
          data-test="locationNameButton"
        />
      </div>
    </div>
    <div class="context-menu">
      <MoreOptionsMenu
        :items="contextMenuItems"
        data-test="context-menu"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { IButtonTextIcon } from '@/types'
import { LocationTemp } from '@/types/locations.d'
import { useLocationStore } from '@/store/Views/locationStore'
import { useMinionsQueries } from '@/store/Queries/minionsQueries'

const props = defineProps<{
  item: LocationTemp,
  openModalForDelete: (item: LocationTemp) => void
}>()

const locationStore = useLocationStore()
const minionsQueries = useMinionsQueries()

const location = computed(() => props.item)

const selectedCard = computed(() => locationStore.selectedLocationId === props.item.id)

const nameBtn = computed<IButtonTextIcon>(() => ({
  label: props.item.location
}))

const contextMenuItems = [
  { label: 'Edit', handler: () => locationStore.selectLocation(props.item.id) },
  { label: 'Delete', handler: () => props.openModalForDelete(props.item) }
]

const getMinionsForLocationId = (locationId: number) => {
  locationStore.getMinionsForLocationId(locationId)
  sartMinionsPoll()
}

// Following functions are for polling new minions every 1 min.
const refreshMinions = async () => {
  await minionsQueries.findMinionsByLocationId().catch(() => console.warn('Could not refresh minions.'))
}

const { resume: sartMinionsPoll, pause: pauseMinionPoll } = useTimeoutPoll(refreshMinions, 60000)

onMounted(() => {
  if (locationStore.selectedLocationId) sartMinionsPoll()
})
onUnmounted(() => pauseMinionPoll())
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';

.locations-card {
  display: flex;
}

.locations-card-items {
  width: 100%;
  display: flex;
  align-items: center;
  gap: var(variables.$spacing-s);
  padding: 0 var(variables.$spacing-s);
  cursor: pointer;
}

.name {
  width: 40%;
}
.context-menu {
  width: 7%;
  display: flex;
  justify-content: flex-end;
}

.selected {
  background-color: var(variables.$shade-4);
}
</style>
