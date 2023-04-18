<template>
  <div class="section-wrapper">
    <div class="section-headline">
      <div
        class="headline"
        data-test="section-headline"
      >
        {{ text }}
      </div>
      <div data-test="infos-slot"><slot name="infos" /></div>
    </div>
    <div
      class="section-actions"
      data-test="actions-slot"
    >
      <slot name="actions" />
    </div>
  </div>
</template>

<script lang="ts" setup>
defineProps({
  text: {
    type: String,
    default: 'Section Headline'
  }
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';

.section-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-headline {
  display: flex;
  align-items: center;
}
.headline {
  @include typography.headline3();
  margin-right: var(variables.$spacing-s);
}
</style>
