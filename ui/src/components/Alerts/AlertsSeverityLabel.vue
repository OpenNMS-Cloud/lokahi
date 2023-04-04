<template>
  <span
    v-if="severity"
    class="severity-status"
    :class="[`${severity.toLowerCase()}-color`]"
    data-test="severity-status"
    >{{ severity }}</span
  >
</template>

<script lang="ts" setup>
defineProps<{
  severity?: string
}>()
</script>

<style lang="scss" scoped>
@use '@/styles/vars.scss';
@use '@/styles/_severities';

.severity-status {
  display: inline-block;
  letter-spacing: normal;
  font-size: 0.65rem;
  line-height: 1.3rem;
  padding: 0 8px;
  border-radius: vars.$border-radius-sm;
}
</style>
