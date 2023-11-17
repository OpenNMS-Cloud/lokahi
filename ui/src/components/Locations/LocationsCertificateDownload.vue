<template>
  <div
    class="download-wrapper"
    data-test="locations-download-cert"
  >
    <div class="download-container">
      <div class="download-title">{{ props.title }}</div>
      <div class="download-buttons-wrapper">
        <span><strong>Note:</strong> If you download a new bundle, you will need to use a new decryption password.</span>
        <div class="download-buttons">
          <ButtonWithSpinner
            primary
            :click="onPrimaryButtonClick"
            :is-fetching="locationStore.certIsFetching"
            data-test="download-btn"
          >
            {{ props.primaryButtonText }}
          </ButtonWithSpinner>
          <FeatherButton
            v-if="props.hasCert"
            secondary
            @click="openModal"
            data-test="revoke-btn"
          >
            {{ props.secondaryButtonText }}
          </FeatherButton>
        </div>
      </div>
    </div>
  </div>
  <div
    class="row mt-m"
    v-if="locationStore.certificatePassword"
  >
    <LocationsMinionCmd />
  </div>
  <DeleteConfirmationModal
    :isVisible="isVisible"
    :customMsg="`Are you sure you want to regenerate the certificate for ${locationStore.selectedLocation?.location}?`"
    :closeModal="() => closeModal()"
    :deleteHandler="() => locationStore.revokeMinionCertificate()"
    actionBtnText="Continue"
  />
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import { useLocationStore } from '@/store/Views/locationStore'
import useModal from '@/composables/useModal'

const locationStore = useLocationStore()
const { openModal, closeModal, isVisible } = useModal()

const props = defineProps({
  title: {
    default: 'Minion Bundle',
    type: String
  },
  primaryButtonText: {
    default: 'Download Bundle',
    type: String
  },
  secondaryButtonText: {
    default: 'Regenerate',
    type: String
  },
  onPrimaryButtonClick: {
    required: true,
    type: Function as PropType<(event: Event) => void>,
    default: () => ({})
  },
  onSecondaryButtonClick: {
    type: Function as PropType<(event: Event) => void>,
    default: () => ({})
  },
  certificatePassword: {
    required: true,
    type: String
  },
  hasCert: {
    default: true,
    type: Boolean
  }
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins';

.download-wrapper {
  width: 100%;
  margin-bottom: 20px;
}
.download-container {
  background-color: var(variables.$border-light-on-warning);
  display: flex;
  flex-direction: column;
  border-radius: 4px;
  padding: var(variables.$spacing-l);
}
.download-title {
  @include typography.subtitle2();
  margin-bottom: var(variables.$spacing-m);
}
.download-buttons {
  display: flex;
  flex-direction: row;
  width: 100%;
}
:deep(.feather-input-sub-text) {
  display: none;
}
.download-buttons-wrapper {
  &.hasCert {
    max-width: unset;
  }

  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  width: 100%;
  gap: var(variables.$spacing-m);
}
</style>
