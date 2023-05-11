<template>
  <div class="locations-edit-form-wrapper">
    <form
      @submit.prevent="onSubmit"
      novalidate
      class="locations-edit-form"
    >
      <HeadlineSection
        :text="formInputs.location"
        data-test="headline"
      >
        <template #right>
          <FeatherButton icon="placeholder">
            <FeatherIcon :icon="placeholder"> </FeatherIcon>
          </FeatherButton>
          <FeatherButton
            @click="deleteLocation"
            icon="Delete"
          >
            <FeatherIcon :icon="Delete"> </FeatherIcon>
          </FeatherButton>
        </template>
      </HeadlineSection>
      <div class="inputs">
        <div class="row">
          <FeatherInput
            label="Location Name *"
            v-model="formInputs.location"
            :schema="nameV"
            required
            class="input-name"
            data-test="input-name"
          >
            <template #pre><FeatherIcon :icon="icons.Location" /></template
          ></FeatherInput>
        </div>
        <div class="row">
          <FeatherInput
            label="Address (optional)"
            v-model="formInputs.address"
            class="input-address"
            data-test="input-address"
          >
            <template #pre> <FeatherIcon :icon="icons.placeholder" /> </template
          ></FeatherInput>
        </div>
        <div class="row">
          <FeatherInput
            label="Longitude (optional)"
            v-model="formInputs.longitude"
            class="input-longitude"
            data-test="input-longitude"
          >
            <template #pre><FeatherIcon :icon="icons.placeholder" /></template
          ></FeatherInput>
          <FeatherInput
            label="Latitude (optional)"
            v-model="formInputs.latitude"
            class="input-latitude"
            data-test="input-latitude"
          >
            <template #pre><FeatherIcon :icon="icons.placeholder" /></template
          ></FeatherInput>
        </div>
      </div>
      <div class="row">
        <div class="box api-key">
          <div class="top">
            <label for="apiKey">API Key:</label>
            <span id="apiKey">ABCD1234@#$%</span>
          </div>
          <ButtonTextIcon
            @click="generateKey"
            :item="generateKeyBtn"
          >
            <template #pre
              ><FeatherIcon
                :icon="ContentCopy"
                aria-hidden="true"
                focusable="false"
                class="icon-pre"
            /></template>
          </ButtonTextIcon>
        </div>
        <div class="box download-credentials">
          <div class="top">
            <div>Location Credentials File Bundle</div>
            <div>4/24/2023 - 00:00</div>
          </div>
          <ButtonTextIcon
            @click="downloadCredentials"
            :item="downloadCredentialsBtn"
          >
            <template #pre
              ><FeatherIcon
                :icon="DownloadFile"
                aria-hidden="true"
                focusable="false"
                class="icon-pre"
            /></template>
          </ButtonTextIcon>
        </div>
      </div>
      <div class="row mt-m">
        <InstructionsStepper
          :text-button="instructions.textButton"
          :step-lists="instructions.stepLists"
        />
      </div>
      <FooterSection>
        <template #buttons>
          <FeatherButton
            @click="locationStore.setDisplayType(DisplayType.LIST)"
            secondary
            data-test="cancel-button"
            >cancel</FeatherButton
          >
          <ButtonWithSpinner
            :isFetching="updateIsFetching"
            type="submit"
            primary
            data-test="save-button"
            >save</ButtonWithSpinner
          >
        </template>
      </FooterSection>
    </form>
  </div>
</template>

<script setup lang="ts">
import Location from '@featherds/icon/action/Location'
import ContentCopy from '@featherds/icon/action/ContentCopy'
import DownloadFile from '@featherds/icon/action/DownloadFile'
import Delete from '@featherds/icon/action/Delete'
import placeholder from '@/assets/placeholder.svg'
import { string } from 'yup'
import { useForm } from '@featherds/input-helper'
import { Location as LocationType } from '@/types/graphql'
import { IButtonTextIcon } from '@/types'
import { DisplayType } from '@/types/locations.d'
import { useLocationStore } from '@/store/Views/locationStore'

const props = defineProps<{
  id: number
}>()

const locationStore = useLocationStore()

const formInputs = computed(() => {
  const selectedLocation = locationStore.locationsList.filter((l: LocationType) => l.id === props.id)[0]

  return {
    id: selectedLocation.id,
    location: selectedLocation.location,
    address: selectedLocation.address,
    longitude: selectedLocation.longitude,
    latitude: selectedLocation.latitude
  }
})

const form = useForm()
const nameV = string().required('Location name is required.')

const updateIsFetching = computed(() => locationStore.updateIsFetching)
const onSubmit = async () => {
  const isFormInvalid = form.validate().length > 0 // array of errors

  if (isFormInvalid) return

  const isFormUpdated = await locationStore.updateLocation(formInputs.value)

  if (isFormUpdated) {
    locationStore.setDisplayType(DisplayType.LIST)
    form.clearErrors()
  }
}

const generateKeyBtn: IButtonTextIcon = {
  label: 'GENERATE KEY'
}
const generateKey = () => ({})

const downloadCredentialsBtn: IButtonTextIcon = {
  label: 'READY TO DOWNLOAD'
}
const downloadCredentials = () => ({})

const deleteLocation = async () => {
  const success = await locationStore.deleteLocation(props.id)

  if (success) {
    locationStore.setDisplayType(DisplayType.LIST)
    form.clearErrors()
  }
}

const instructions = {
  textButton: 'Need instructions for minion deployment at this location?',
  stepLists: [
    {
      title: 'Step 1 heading',
      items: [
        'Enter configuration commands, one per line. End with CNTL/Z',
        'Router-Dallas(config)#logging 192.168.0.30',
        'Router-Dallas(config)#service timestamps debug datetime localtime show-timezone'
      ]
    },
    {
      title: 'Step 2 heading',
      items: ['Instruction 2...', 'Instruction 2a...', 'Instruction 2b...']
    },
    {
      title: 'Step 3 heading',
      items: ['Instruction 3...', 'Instruction 3a...', 'Instruction 3b...']
    },
    {
      title: 'Step 4 heading',
      items: ['Instruction 4...', 'Instruction 4a...', 'Instruction 4b...']
    }
  ]
}

onUnmounted(() => {
  locationStore.selectLocation(undefined)
  form.clearErrors()
})

const icons = markRaw({
  Location,
  ContentCopy,
  DownloadFile,
  Delete,
  placeholder
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/mixins.scss';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';

.locations-edit-form-wrapper {
  @include mixins.wrapper-on-background;

  .row {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    margin-bottom: var(variables.$spacing-xs);
    :deep(.feather-icon) {
      &.error-icon {
        color: var(variables.$error);
      }
    }
    > * {
      width: 100%;
    }

    @include mediaQueriesMixins.screen-sm {
      > *:not(.input-address) {
        width: 49%;
      }
    }
    @include mediaQueriesMixins.screen-md {
      > * {
        width: 100%;
      }
    }
    @include mediaQueriesMixins.screen-lg {
      > *:not(.input-address) {
        width: 49%;
      }
    }
  }

  .row.box {
    margin-bottom: var(variables.$spacing-m);
  }

  .box {
    padding: var(variables.$spacing-s) var(variables.$spacing-l);
    background: var(variables.$shade-4);
    border-radius: vars.$border-radius-xs;
    > .top {
      margin-bottom: var(variables.$spacing-s);
    }
    &.api-key {
      > .top > label {
        margin-right: var(variables.$spacing-xs);
      }
    }
    &.download-credentials {
      > .top {
        display: flex;
        justify-content: space-between;
      }
    }
    .icon-pre {
      margin-right: var(variables.$spacing-s);
    }
    .icon-post {
      margin-left: var(variables.$spacing-l);
    }
  }
}
</style>
