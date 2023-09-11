<template>
  <LIcon>
    <div class="pin">
      <div
        class="status-circle"
        :class="`${status.toLowerCase()}`"
      ></div>
    </div>
  </LIcon>
</template>

<script setup lang="ts">
import { LIcon } from '@vue-leaflet/vue-leaflet'

defineProps({
  status: { type: String, default: 'critical' }
})
</script>

<style lang="scss" scoped>
@use '@/styles/vars.scss';
@use '@/styles/_severities.scss';
.pin {
  background: #fff;

  .status-circle {
    width: 20px;
    height: 20px;
    border-radius: vars.$border-radius-round;
  }
}
</style>
