<template>
  <div class="links">
    <FeatherButton
      @click="redirect('/appliances')"
      secondary
      >Add Device</FeatherButton
    >
    <FeatherButton
      @click="redirect('/discovery')"
      secondary
      >New Discovery</FeatherButton
    >
    <FeatherButton
      @click="redirect('/monitoring-policies')"
      secondary
      >Add Monitory Policy</FeatherButton
    >
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
.links {
  .btn,
  .btn-secondary {
    margin-left: 0;
    margin-bottom: var(variables.$spacing-s);
    margin-right: var(variables.$spacing-s);
  }
  @include mediaQueriesMixins.screen-md {
    .btn,
    .btn-secondary {
      margin-left: 0;
      margin-bottom: var(variables.$spacing-s);
      margin-right: var(variables.$spacing-s);
    }
  }
}
</style>
