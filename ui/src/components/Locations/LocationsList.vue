<template>
  <div class="locations-list-wrapper">
    <HeadlineSection
      text="locations"
      data-test="headline"
    >
      <template #left>
        <CountColor
          :count="locationsList?.length || 0"
          data-test="count"
        />
      </template>
      <template #right>
        <FeatherIcon
          :icon="icons.Help"
          data-test="icon-help"
          @click="$emit('showInstructions')"
          class="pointer"
        />
      </template>
    </HeadlineSection>
    <div class="locations-list">
      <div class="header" data-test="header">
        <div class="name">
          Name
        </div>
        <!-- Post EAR -->
        <!-- <div
          class="status"
          data-test="status"
        >
          Status
        </div> -->
      </div>
      <ul>
        <li
          v-for="(item, index) in locationsList"
          :key="index"
          data-test="card"
        >
          <LocationsCard :item="item" :openModalForDelete="openModalForDelete" />
        </li>
      </ul>
    </div>
  </div>
  <DeleteConfirmationModal
    :isVisible="isVisible"
    :name="locationToDelete?.location"
    :closeModal="() => closeModal()"
    :deleteHandler="() => deleteLocation()"
    :isDeleting="locationStore.isDeleting"
  />
</template>

<script setup lang="ts">
import HeadlineSection from '@/components/Common/HeadlineSection.vue'
import Help from '@featherds/icon/action/Help'
import { LocationTemp } from '@/types/locations.d'
import useModal from '@/composables/useModal'
import { useLocationStore } from '@/store/Views/locationStore'
import { useMinionsQueries } from '@/store/Queries/minionsQueries'
import { MonitoringLocation } from '@/types/graphql'

const props = defineProps<{
  items: LocationTemp[]
}>()

defineEmits(['show-instructions'])

const locationStore = useLocationStore()
const minionsQueries = useMinionsQueries()
const { openModal, closeModal, isVisible } = useModal()

const locationToDelete = ref<MonitoringLocation>()
const locationsList = computed(() => props.items)

const deleteLocation = async () => {
  if (locationToDelete.value) {
    await locationStore.deleteLocation(locationToDelete.value.id)
    await minionsQueries.refreshMinionsById()
  }
}

const openModalForDelete = (item: MonitoringLocation) => {
  locationToDelete.value = item
  openModal()
}

const icons = markRaw({
  Help
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars.scss';

.locations-list-wrapper {
  padding: var(variables.$spacing-m) var(variables.$spacing-s);
  background: var(variables.$surface);
  border-radius: vars.$border-radius-surface;
  border: 1px solid var(variables.$border-on-surface);

  .header {
    display: flex;
    align-items: center;
    gap: var(variables.$spacing-s);
    padding: var(variables.$spacing-xs) var(variables.$spacing-s);
    background-color: var(variables.$background);
    > * {
      &:nth-child(1) {
        width: 40%;
      }
      &:nth-child(2) {
        width: 30%;
        display: flex;
        justify-content: center;
      }
    }
  }
}
</style>
