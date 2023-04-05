<template>
  <div class="container">
    <div class="header">
      <PageHeadline text="Insights Dashboard" />
      <HeaderLinks />
    </div>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped lang="scss">
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/themes/variables';
@use '@/styles/mediaQueriesMixins.scss';

.container {
  display: flex;
  margin: 0 1rem;
  padding: var(variables.$spacing-l) var(variables.$spacing-m);

  > .header {
    @include typography.headline2();
    display: flex;
    align-items: center;
    flex-direction: column;
    width: 100%;

    @include mediaQueriesMixins.screen-md {
      flex-direction: row;
      justify-content: space-between;
    }
  }
}
</style>
