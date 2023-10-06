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
          <FeatherButton icon="map">
            <FeatherIcon :icon="icons.Location"> </FeatherIcon>
          </FeatherButton>
          <FeatherButton
            @click="openModal"
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
            :disabled="true"
            class="input-name"
            data-test="input-name"
          >
            <template #pre><FeatherIcon :icon="icons.Location" /></template
          ></FeatherInput>
        </div>
        <div class="row">
          <AddressAutocomplete
            :addressModel="formInputs"
            class="full-width"
            @onAddressChange="onAddressChange"
          />
        </div>
        <!-- <div class="row">
          <FeatherInput
            label="Latitude (optional)"
            v-model="formInputs.latitude"
            class="input-latitude"
            data-test="input-latitude"
          />
          <FeatherInput
            label="Longitude (optional)"
            v-model="formInputs.longitude"
            class="input-longitude"
            data-test="input-longitude"
          />
        </div> -->
      </div>
      <div>
        <LocationsCertificateDownload
          :certificate-password="locationStore.certificatePassword"
          :on-primary-button-click="downloadCert"
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
  <DeleteConfirmationModal
    :isVisible="isVisible"
    :name="locationStore.selectedLocation?.location"
    :closeModal="() => closeModal()"
    :deleteHandler="() => deleteLocation()"
    :isDeleting="locationStore.isDeleting"
  />
</template>

<script setup lang="ts">
import Location from '@featherds/icon/action/Location'
import ContentCopy from '@featherds/icon/action/ContentCopy'
import DownloadFile from '@featherds/icon/action/DownloadFile'
import Delete from '@featherds/icon/action/Delete'
import { string } from 'yup'
import { useForm } from '@featherds/input-helper'
import { MonitoringLocationUpdateInput } from '@/types/graphql'
import { DisplayType } from '@/types/locations.d'
import { useLocationStore } from '@/store/Views/locationStore'
import { createAndDownloadBlobFile } from '@/components/utils'
import { useMinionsQueries } from '@/store/Queries/minionsQueries'
import useModal from '@/composables/useModal'

const props = defineProps<{
  id: number
}>()

const locationStore = useLocationStore()
const minionsQueries = useMinionsQueries()
const { openModal, closeModal, isVisible } = useModal()
const formInputs = reactive({} as Required<MonitoringLocationUpdateInput>)

watchEffect(() => {
  formInputs.id = locationStore.selectedLocation?.id
  formInputs.location = locationStore.selectedLocation?.location
  formInputs.address = locationStore.selectedLocation?.address
  formInputs.longitude = locationStore.selectedLocation?.longitude
  formInputs.latitude = locationStore.selectedLocation?.latitude
})

const onAddressChange = (newAddress: any) => {
  if (newAddress?.value.label && newAddress?.value.x && newAddress?.value.y) {
    formInputs.address = newAddress.value.label
    formInputs.longitude = newAddress.value.x
    formInputs.latitude = newAddress.value.y
  }
}

const form = useForm()
const nameV = string().required('Location name is required.')

const updateIsFetching = computed(() => locationStore.updateIsFetching)
const onSubmit = async () => {
  const isFormInvalid = form.validate().length > 0 // array of errors

  if (isFormInvalid) return

  const isFormUpdated = await locationStore.updateLocation(formInputs)

  if (isFormUpdated) {
    locationStore.setDisplayType(DisplayType.LIST)
    form.clearErrors()
  }
}

const downloadCert = async () => {
  const minionCertificate = await locationStore.getMinionCertificate()

  if (minionCertificate) {
    locationStore.setCertificatePassword(minionCertificate.password as string)
    createAndDownloadBlobFile(minionCertificate.certificate, `minion-${formInputs.location}.zip`)
    form.clearErrors()
  }
}

const deleteLocation = async () => {
  const success = await locationStore.deleteLocation(props.id)
  await minionsQueries.refreshMinionsById()

  if (success) {
    form.clearErrors()
  }
}

onUnmounted(() => {
  form.clearErrors()
})

const icons = markRaw({
  Location,
  ContentCopy,
  DownloadFile,
  Delete
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
      > *:not(.full-width) {
        width: 49%;
      }
    }
    @include mediaQueriesMixins.screen-md {
      > * {
        width: 100%;
      }
    }
    @include mediaQueriesMixins.screen-lg {
      > *:not(.full-width) {
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
