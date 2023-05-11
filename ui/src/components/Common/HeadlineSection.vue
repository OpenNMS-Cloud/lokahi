<template>
  <div class="headline-section">
    <div
      class="left"
      data-test="left"
    >
      <div
        class="headline"
        data-test="headline"
      >
        {{ text }}
      </div>
      <slot name="left" />
    </div>
    <div
      class="middle"
      data-test="middle"
    >
      <slot name="middle" />
    </div>
    <div
      class="right"
      data-test="right"
    >
      <slot name="right" />
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

.headline-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(variables.$spacing-m);
  gap: 5%;

  .headline {
    display: flex;
    @include typography.headline3();
    margin-right: var(variables.$spacing-s);
    text-transform: capitalize;
  }

  .left {
    width: 40%;
    display: flex;
    align-items: center;
  }

  .middle {
    width: 35%;
    display: flex;
    align-items: center;
  }

  .right {
    width: 15%;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    :deep(.feather-icon) {
      width: 1.5rem;
      height: 1.5rem;
    }
  }
}
</style>
