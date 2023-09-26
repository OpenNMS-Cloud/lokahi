<template>
  <span
    class="feather-text-badge"
    :class="`${type}-color`"
  >
    <slot />
  </span>
</template>
<script setup lang="ts">
defineProps({
  type: { type: String, required: true }
})
</script>
<style scoped lang="scss">
@use '@/styles/_severities.scss';
@import '@featherds/styles/themes/variables';
@import '@featherds/styles/themes/utils';
@import '@featherds/styles/mixins/typography';

.feather-text-badge {
  height: 1.25rem;
  padding: 0.125rem 0.5rem;
  display: inline-flex;
  align-items: center;
  border-radius: 6px;
  @include caption;
  font-weight: var($font-bold);
  margin-left: 0.5rem;
  text-transform: capitalize;
}
</style>
