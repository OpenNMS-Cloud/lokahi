<template>
  <div class="container">
    <div class="title">Run Minion with the Run Command in a Terminal Window</div>
    <span
      >Install our minion by navigating to your chosen directory in a terminal and typing the following
      command:</span
    >
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
      <pre>
      docker compose up -d
    </pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useLocationStore } from '@/store/Views/locationStore'
import useSnackbar from '@/composables/useSnackbar'
import CopyIcon from '@featherds/icon/action/ContentCopy'

const { showSnackbar } = useSnackbar()
const locationStore = useLocationStore()

const copyClick = () => {
  const cmd = 'docker compose up -d'
  navigator.clipboard
    .writeText(cmd)
    .then(() => {
      showSnackbar({
        msg: 'Minion run command copied.'
      })
    })
    .catch(() => {
      showSnackbar({
        msg: 'Failed to copy command.'
      })
    })
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/themes/variables';

.container {
  background-color: var(variables.$surface);
  padding: 10px;
  .title {
    @include typography.subtitle1();
  }
  .instructions {
    background-color: var(variables.$background);
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
      margin-top: -7px;
    }
  }
}
</style>
