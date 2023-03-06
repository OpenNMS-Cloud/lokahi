<template>
  <h2 class="spacing">{{ heading }}</h2>
</template>

<script lang="ts" setup>
defineProps({
  heading: {
    type: String,
    default: 'Page Heading'
  }
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';

.spacing {
  margin-top: var(variables.$spacing-xl);
  margin-bottom: var(variables.$spacing-xl);
}
</style>
