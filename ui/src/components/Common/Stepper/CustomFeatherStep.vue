<template>
  <div class="container">
    <div class="title" v-if="title">{{ title }}</div>
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
defineEmits([
  'slideNext',
  'slidePrev'
])

defineProps({
  nextBtnText: String,
  prevBtnText: String,
  hideNextBtn: Boolean,
  disableNextBtn: Boolean,
  title: String
})
</script>

<style scoped lang="scss">
@use "@featherds/styles/mixins/typography";
@use "@featherds/styles/themes/variables";
.container {
  padding: var(variables.$spacing-l) 0;

  .title {
    @include typography.headline4;
  }
}
</style>
