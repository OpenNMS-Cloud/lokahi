<template>
  <FeatherButton 
    primary
    data-test="add-device-btn" 
    @click="openModal"
  > 
    <template v-slot:icon>
      <FeatherIcon 
        :icon="Add" 
        aria-hidden="true" 
        focusable="false" 
      />
      Add Device
    </template>
  </FeatherButton>

  <PrimaryModal title="Add Device" :visible="isVisible">
    <template v-slot:content>
      <FeatherInput
        data-test="name-input"
        label="Name"
        v-model="device.label"
      />
      <FeatherSelect
        data-test="location-name-input"
        name="locationOptions"
        v-model="locationOption"
        :options="applianceQueries.locations"
        text-prop="name"
        @update:modelValue="selectLocation"
        label="Location"
      />
      <FeatherInput
        data-test="ip-input"
        label="Management IP"
        v-model="device.managementIp"
      />
      <FeatherInput
        data-test="string-input"
        label="Community String (Optional)"
        v-model="device.snmpCommunityString"
      />
      <FeatherInput
        type="number"
        data-test="port-input"
        label="SNMP Port (Optional)"
        v-model="device.port"
      />
      <FeatherInput
        type="number"
        data-test="port-latitude"
        label="Latitude (Optional)"
        v-model="device.latitude"
      />
      <FeatherInput
        type="number"
        data-test="port-longitude"
        label="Longitude (Optional)"
        v-model="device.longitude"
      />
    </template>

    <template v-slot:footer>
      <FeatherButton 
        data-test="cancel-btn" 
        secondary 
        @click="cancel">
          Cancel
      </FeatherButton>
      
      <FeatherButton 
        data-test="save-btn" 
        primary
        :disabled="!device.label" 
        @click="save">
          Save
      </FeatherButton>
    </template>
  </PrimaryModal>
</template>

<script setup lang="ts">
import Add from '@featherds/icon/action/Add'
import { useDeviceMutations } from '@/store/Mutations/deviceMutations'
import { useAppliancesQueries } from '@/store/Queries/appliancesQueries'
import useModal from '@/composables/useModal'
import useSnackbar from '@/composables/useSnackbar'
import { DeviceCreateDtoInput } from '@/types/graphql'

const { showSnackbar } = useSnackbar()
const { openModal, closeModal, isVisible } = useModal()
const deviceMutations = useDeviceMutations()
const applianceQueries = useAppliancesQueries()

const defaultDevice: DeviceCreateDtoInput = { 
  label: undefined,
  location: undefined,
  latitude: undefined,
  longitude: undefined,
  monitoringArea: undefined,
  managementIp: undefined,
  port: undefined,
  snmpCommunityString: undefined
}

const device = reactive({ ...defaultDevice })

const save = async () => {
  // convert the field value to undefined if their value is an empty string (entered then erased the input field returns an empty string) - empty string as value in payload causes error when adding device.
  Object.assign(device, {
    port: device.port || undefined,
    latitude: device.latitude || undefined,
    longitude: device.longitude || undefined
  })

  await deviceMutations.addDevice({ device })
  
  if (!deviceMutations.error) {
    // clears device obj on successful save
    Object.assign(device, defaultDevice)

    closeModal()

    showSnackbar({
      msg: 'Device successfuly saved.'
    })

    // Timeout because device may not be available right away
    // TODO: Replace timeout with websocket/polling
    setTimeout(() => {
      applianceQueries.fetchDevicesForTable()
    }, 350)
  }
}

const cancel = () => {
  // clears device obj
  Object.assign(device, defaultDevice)

  closeModal()
}

// stores the selected location option
const locationOption = ref()
// sets location val in the payload
const selectLocation = () => {
  if (locationOption.value?.name) {
    device.location = locationOption.value.name
  }
}
// sets default location when locations available
watchEffect(() => { 
  locationOption.value = applianceQueries.locations[0]
  selectLocation()
})
</script>

<style scoped lang="scss">
@import "@featherds/styles/mixins/typography";

.title {
  @include headline3;
  text-align: center;
  margin-bottom: 15px;
}
</style>