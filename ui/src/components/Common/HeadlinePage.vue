<template>
  <div
    class="page-headline"
    data-test="page-headline"
  >
    {{ text }}
  </div>
</template>

<script lang="ts" setup>
defineProps({
  text: {
    type: String,
    default: 'Page Headline'
  }
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.page-headline {
  @include typography.headline1();
  margin-top: 40px;
  margin-bottom: 40px;
}
</style>
