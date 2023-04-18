<template>
  <FeatherAppLayout
    contentLayout="full"
    class="feather-styles layout"
  >
    <template v-slot:header>
      <Menubar />
    </template>

    <template v-slot:rail>
      <NavigationRail />
    </template>

    <div id="mainContent">
      <Spinner />
      <Snackbar />
      <router-view />
    </div>
  </FeatherAppLayout>
</template>

<style lang="scss" scoped>
:deep(.feather-app-rail) {
  border-right: 0 !important;
}
</style>

<style lang="scss">
@use '@/styles/_app';
</style>
