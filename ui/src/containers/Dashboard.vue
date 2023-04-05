<template>
  <div class="container">
    <div class="header">
      <PageHeadline text="Insights Dashboard" />
      <HeaderLinks />
    </div>
    <div class="section-title">Alert Status</div>
    <div class="list-alerts">
      <AlertsSeverityFilters @click="redirect('/alerts')" />
    </div>
  </div>
</template>

<script setup lang="ts">
const router = useRouter()

const redirect = (route: string) => {
  router.push(route)
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/themes/variables';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';

.container {
  display: flex;
  margin: 0 1rem;
  flex-direction: column;
  width: 100%;
  padding: var(variables.$spacing-l) var(variables.$spacing-m);
  margin: auto;
  @include mediaQueriesMixins.screen-md {
    width: vars.$max-width-constrained;
  }
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
  .list-alerts {
    overflow-x: auto;
  }
  .section-title {
    @include typography.headline3();
  }
}
</style>
