<template>
  <div class="instructions">
    <FeatherButton
      :disabled="!locationStore.certificatePassword"
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
    <p>Make sure you replace <strong>PATH_TO_DOWNLOADED_FILE</strong> with the full path to the certificate file.</p>
    <pre>
      {{ locationStore.minionDockerCmd }}
    </pre>
  </div>
</template>

<script setup lang="ts">
import { useLocationStore } from '@/store/Views/locationStore'
import useSnackbar from '@/composables/useSnackbar'
import CopyIcon from '@featherds/icon/action/ContentCopy'

const { showSnackbar } = useSnackbar()
const locationStore = useLocationStore()

const copyClick = () => {
  navigator.clipboard.writeText(locationStore.minionDockerCmd).then(() => {
    showSnackbar({
      msg: 'Minion run command copied.'
    })
  }).catch(() => {
    showSnackbar({
      msg: 'Failed to copy command.'
    })
  })
}
</script>

<style lang="scss" scoped>
.instructions {
  background-color: #e5f4f9;
  padding: 12px 24px;
  p {
    margin-bottom: 10px;
  }
  pre {
    margin: 0;
    white-space: normal;
    word-wrap: break-word;
  }
  .download-copy-button {
    float: right;
  }
}
</style>
