<template>
  <div class="full-page-container">
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
          @click="locationStore.addLocation"
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
          @showInstructions="openInstructions(InstructionsType.Location)"
        />
      </div>
      <div class="content-right">
        <LocationsMinionsList
          v-if="locationStore.displayType === DisplayType.LIST"
          :minions="minionsList"
          @showInstructions="openInstructions(InstructionsType.Minion)"
        />
        <LocationsAddForm
          v-if="locationStore.displayType === DisplayType.ADD"
          data-test="location-add-form"
        />
        <LocationsEditForm
          v-if="locationStore.displayType === DisplayType.EDIT"
          :id="selectedLocationId"
        />
        <LocationsCertificateDownload
          v-if="locationStore.displayType === DisplayType.READY"
          :certificate-password="locationStore.certificatePassword"
          :on-primary-button-click="downloadCert"
          :has-cert="false"
        />
      </div>
    </div>
  </div>
  <LocationsInstructions
    :instructionsType='instructionsType'
    :isOpen="showInstructions"
    @drawerClosed="() => (showInstructions = false)"
  />
</template>

<script lang="ts" setup>
import Add from '@featherds/icon/action/Add'
import Search from '@featherds/icon/action/Search'
import Help from '@featherds/icon/action/Help'
import { useLocationStore } from '@/store/Views/locationStore'
import { useMinionsQueries } from '@/store/Queries/minionsQueries'
import LocationsList from '@/components/Locations/LocationsList.vue'
import { DisplayType } from '@/types/locations.d'
import { createAndDownloadBlobFile } from '@/components/utils'
import { InstructionsType } from '@/components/Locations/locations.constants'

const locationStore = useLocationStore()
const minionsQueries = useMinionsQueries()
const showInstructions = ref(false)
const instructionsType = ref(InstructionsType.Location)

const locationsList = computed(() => locationStore.locationsList)
const minionsList = computed(() => minionsQueries.minionsList)

onMounted(async () => await locationStore.fetchLocations())

const selectedLocationId = computed(() => locationStore.selectedLocationId)

const searchLocationListener = async (val: string | number | undefined) => {
  await locationStore.searchLocations(val as string)
}

const icons = markRaw({
  Add,
  Search,
  Help
})

const downloadCert = async () => {
  const minionCertificate = await locationStore.getMinionCertificate()

  if (minionCertificate) {
    locationStore.setCertificatePassword(minionCertificate.password as string)
    createAndDownloadBlobFile(minionCertificate.certificate, `minion-${locationStore.selectedLocation?.location}.zip`)
  }
}

const openInstructions = (type: InstructionsType) => {
  showInstructions.value = true
  instructionsType.value = type
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/layout/headlineTwoColumns';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';

.full-page-container {
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
