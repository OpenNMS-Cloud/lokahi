<template>
  <div class="wrapper">
    <div class="header">
      <HeadlinePage
        text="Locations"
        data-test="locations-headline"
      />
      <div data-test="locations-notification-ctrl"><AppliancesNotificationsCtrl /></div>
    </div>
    <div class="content">
      <div class="content-left">
        <FeatherButton
          primary
          @click="addLocation"
          data-test="add-location-btn"
        >
          <FeatherIcon :icon="icons.Add" />
          Location
        </FeatherButton>
        <hr />
        <FeatherInput
          @update:model-value="searchLocationListener"
          label="Search Location"
          type="search"
          class="search-location-input"
          data-test="search-input"
        >
          <template #pre>
            <FeatherIcon :icon="icons.Search" />
          </template>
        </FeatherInput>
        <LocationsList
          v-if="locationsList"
          :items="locationsList"
          @showInstructions="showInstructions = true"
        />
      </div>
      <div class="content-right">
        <LocationsMinionsList
          v-if="locationStore.displayType === DisplayType.LIST && minionsList"
          :minions="minionsList"
          @showInstructions="showInstructions = true"
        />
        <LocationsAddForm
          v-if="locationStore.displayType === DisplayType.ADD"
          data-test="location-add-form"
        />
        <LocationsEditForm
          v-if="locationStore.displayType === DisplayType.EDIT"
          :id="selectedLocationId"
        />
      </div>
    </div>
  </div>
  <LocationsInstructions
    :isOpen="showInstructions"
    @drawerClosed="() => (showInstructions = false)"
  />
</template>

<script lang="ts" setup>
import Add from '@featherds/icon/action/Add'
import Search from '@featherds/icon/action/Search'
import Help from '@featherds/icon/action/Help'
import { useLocationStore } from '@/store/Views/locationStore'
import LocationsList from '@/components/Locations/LocationsList.vue'
import { DisplayType } from '@/types/locations.d'

const locationStore = useLocationStore()
const showInstructions = ref(false)

const locationsList = computed(() => locationStore.locationsList)
const minionsList = computed(() => locationStore.minionsList)

onMounted(async () => {
  await locationStore.fetchLocations()
  await locationStore.fetchMinions()
})

const addLocation = () => {
  locationStore.setDisplayType(DisplayType.ADD)
}

const selectedLocationId = computed(() => locationStore.selectedLocationId)

const searchLocationListener = async (val: string | number | undefined) => {
  await locationStore.searchLocations(val as string)
}

const icons = markRaw({
  Add,
  Search,
  Help
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/layout/headlineTwoColumns';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';

.wrapper {
  .content-left {
    .search-location-input {
      width: 100%;

      @include mediaQueriesMixins.screen-md {
        width: 100%;
      }
      @include mediaQueriesMixins.screen-xl {
        width: 50%;
      }
    }
  }
}
</style>
