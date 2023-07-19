<template>
  <AtomicAutocomplete
    class="locations"
    inputLabel="Search Locations"
    :loading="locationStore.isSearching"
    :outsideClicked="closeAutocomplete"
    :itemClicked="itemClicked"
    :resultsVisible="isAutoCompleteOpen"
    :focusLost="onFocusLost"
    :wrapperClicked="wrapperClicked"
    :results="locationStore.locationsList.map((d) => d.location)"
    :inputValue="inputValue"
    :textChanged="textChanged"
    :errMsg="errMsg"
    :disabled="disabled"
    :allowNew="false"
  />

  <FeatherChipList
    v-if="discoveryStore.selectedLocation"
    label="Locations"
    class="loc-chip-list"
  >
    <FeatherChip class="pointer">
      <template v-slot:icon>
        <FeatherIcon
          v-if="!disabled"
          @click="removeLocation"
          :icon="cancelIcon"
        />
      </template>
      {{ discoveryStore.selectedLocation.location }}
    </FeatherChip>
  </FeatherChipList>
</template>

<script setup lang="ts">
import useAtomicAutocomplete from '@/composables/useAtomicAutocomplete'
import { useLocationStore } from '@/store/Views/locationStore'
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
import { useValidation } from '@featherds/input-helper'
import CancelIcon from '@featherds/icon/navigation/Cancel'
import { string } from 'yup'
import { Ref } from 'vue'
import { IValidationFailure } from '@featherds/input-helper/src/composables/useForm'

defineProps<{
  disabled?: boolean
}>()

const cancelIcon = markRaw(CancelIcon)

const locationStore = useLocationStore()
const discoveryStore = useDiscoveryStore()
const validationResults = ref()
const allErrorMsgs = inject<Ref<IValidationFailure[]>>('featherFormErrors')
const locationErrId = 'no-location'

const errMsg = computed<string>(() => {
  if (allErrorMsgs?.value) {
    const noLocationMsg = allErrorMsgs.value.filter((error: any) => error.inputId === locationErrId)[0]
    if (noLocationMsg) {
      return noLocationMsg.message
    }
  }

  if (validationResults.value?.success === false) {
    return validationResults.value?.message
  }
})

const selectLocation = (item: { name: string }) => {
  for (const loc of locationStore.locationsList) {
    if (loc.location === item.name) {
      discoveryStore.selectedLocation = loc
      validationResults.value = schema.validate()
      return
    }
  }
}

const removeLocation = () => {
  discoveryStore.selectedLocation = undefined
  validationResults.value = schema.validate()
}

const { closeAutocomplete, itemClicked, isAutoCompleteOpen, onFocusLost, wrapperClicked, textChanged, inputValue } =
  useAtomicAutocomplete(locationStore.searchLocations, () => locationStore.locationsList.length, selectLocation)

const locationV = string().test(
  locationErrId,
  'Location is required.',
  () => discoveryStore.selectedLocation !== undefined
)

const schema = useValidation(
  ref(locationErrId),
  toRef(discoveryStore.selectedLocation?.location),
  'Location',
  locationV
)

const marginBottom = computed(() =>
  discoveryStore.selectedLocation || validationResults.value?.success === false ? '10px' : '25px'
)

onMounted(async () => {
  await locationStore.searchLocations()
  if (locationStore.locationsList.length === 1) {
    selectLocation({ name: locationStore.locationsList[0].location! })
  }
})
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
.locations {
  margin-bottom: v-bind(marginBottom);
}

.loc-chip-list {
  margin: 0 0 10px 0;
}

:deep(.atomic-input-wrapper) {
  height: 40px;
}
</style>
