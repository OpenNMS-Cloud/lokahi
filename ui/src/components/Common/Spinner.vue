<template>
  <transition name="fade">
    <div class="spinner" v-if="isActive">
      <FeatherSpinner />
    </div>
  </transition>
</template>

<script setup lang="ts">
import useSpinner from '@/composables/useSpinner'
const { isActive } = toRefs(useSpinner())
</script>

<style scoped lang="scss">
@use "@/styles/_transitionFade";

.spinner {
  z-index: 2;
  position: absolute;
  width: 100%;
  background: transparent;
}
.spinner-container {
  height: 78vh;
}
</style>
