<template>
  <FeatherAppLayout contentLayout="full" class="feather-styles layout">
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
// Remove KC redirectUri theme param
const route = useRoute()
const router = useRouter()
if (route.query.theme) router.replace(route.path)
</script>

<style lang="scss" scoped>
:deep(.feather-app-rail) {
  border-right: 0 !important;
}
</style>

<style lang="scss">
@use "@/styles/_app";
</style>

