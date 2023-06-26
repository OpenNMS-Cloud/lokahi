<template>
  <FeatherAppLayout contentLayout="full" class="feather-styles layout" v-if="welcomeStore.ready" data-test="main-content">
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

<script setup lang="ts">
import { useWelcomeStore } from '@/store/Views/welcomeStore'
const welcomeStore = useWelcomeStore()

onMounted(async () => {
  await welcomeStore.init()
})
</script>

<style lang="scss" scoped>
:deep(.feather-app-rail) {
  border-right: 0 !important;
}
</style>

<style lang="scss">
@use '@/styles/_app';
</style>
