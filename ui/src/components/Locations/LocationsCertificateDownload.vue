<template>
  <div
    class="download-wrapper"
    data-test="locations-download-cert"
  >
    <div class="download-container">
      <div class="download-title">{{ props.title }}</div>
      <div class="download-functionality">
        <div class="certificate-stats">
          <div class="certificate-data">
            <FeatherIcon
              class="cert-icon"
              :icon="CheckIcon"
            />
            <div>Start Date: 05-08-2023</div>
          </div>
          <div class="certificate-data">
            <FeatherIcon
              class="cert-icon"
              :icon="CalendarIcon"
            />
            <div>Expire on 05-08-2024</div>
          </div>
        </div>
        <div class="divider"></div>
        <div class="download-buttons-wrapper">
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

          <div class="input-wrapper">
            <FeatherInput
              :label="inputPlaceholder"
              :modelValue="certificatePassword"
              type="text"
              class="download-input"
              :disabled="true"
              data-test="download-input"
            />
            <FeatherButton
              :disabled="!certificatePassword"
              text
              @click="copyClick"
              class="download-copy-button"
            >
              <template v-slot:icon>
                <FeatherIcon
                  :icon="CopyIcon"
                  aria-hidden="true"
                  focusable="false"
                />
                Copy
              </template>
            </FeatherButton>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="row mt-m" v-if="locationStore.certificatePassword">
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
import CopyIcon from '@featherds/icon/action/ContentCopy'
import CheckIcon from '@featherds/icon/action/CheckCircle'
import CalendarIcon from '@featherds/icon/action/CalendarEndDate'
import { useLocationStore } from '@/store/Views/locationStore'
import useModal from '@/composables/useModal'

import useSnackbar from '@/composables/useSnackbar'
const { showSnackbar } = useSnackbar()
const locationStore = useLocationStore()
const { openModal, closeModal, isVisible } = useModal()

const props = defineProps({
  title: {
    default: 'Certificate Status',
    type: String
  },
  primaryButtonText: {
    default: 'Download Certificate',
    type: String
  },
  secondaryButtonText: {
    default: 'Revoke/Regenerate',
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
  inputPlaceholder: {
    default: 'Unlock File Password',
    type: String
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

const copyClick = () => {
  navigator.clipboard.writeText(props.certificatePassword).catch(() => 'Copy to clipboard was unsuccessful.')
  showSnackbar({
    msg: 'Certificate password copied.'
  })
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
@use '@/styles/mediaQueriesMixins';

.download-wrapper {
  width: 100%;
}
.download-container {
  background-color: var(variables.$border-light-on-warning);
  display: flex;
  flex-direction: column;
  border-radius: 4px;
  padding: var(variables.$spacing-l);
}
.download-functionality {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  width: 100%;
  flex-wrap: wrap;
  gap: var(variables.$spacing-l);
  @include mediaQueriesMixins.screen-xl {
    flex-direction: row;
    justify-content: flex-start;
    gap: 60px;
  }

  :deep(.btn-primary) {
    max-width: 285px;
  }
}
.input-wrapper {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  flex: 1 1 0;
  gap: var(variables.$spacing-xs);
}
.divider {
  border: 1px solid #c4c4c4;
  height: 120px;
  width: 1px;
  display: none;

  @include mediaQueriesMixins.screen-xl {
    display: block;
  }
}
.download-title {
  @include typography.subtitle2();
  margin-bottom: var(variables.$spacing-m);
}
.download-copy-button {
  min-width: 87px;
  margin-top: -25px;
}
.download-input {
  width: 285px;
}
.download-buttons {
  display: flex;
  flex-direction: row;
}
.certificate-stats {
  display: flex;
  flex-direction: column;
  gap: var(variables.$spacing-xxs);

  div {
    @include typography.body-small;
  }
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
  max-width: 450px;
}
.cert-icon {
  width: 20px;
  height: 20px;
}
.certificate-data {
  display: flex;
  align-items: center;
  gap: var(variables.$spacing-xs);
}
</style>
