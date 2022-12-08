<template>
  <FeatherAppLayout contentLayout="full" class="feather-styles layout">
    <template v-slot:header>
      <Menubar />
    </template>

    <template v-slot:rail>
      <NavigationRail />
    </template>
    
    <div class="content-and-widget">
      <div id="mainContent" class="main-content">
        <Spinner />
        <Snackbar />
        <router-view />
      </div>
      <transition name="fade">
        <Widgetbar v-if="store.widgetBarOpen" />
      </transition>
    </div>
  </FeatherAppLayout>
</template>
  
<script setup lang="ts">
import { useLayoutStore } from './store/Views/layoutStore'

// Remove KC redirectUri theme param
const route = useRoute()
const router = useRouter()
if (route.query.theme) router.replace(route.path)

const store = useLayoutStore()
const mainContentWidth = computed(() => store.widgetBarOpen ? 'calc(100% - 500px)' : '100%')
</script>

<style lang="scss" scoped>
.content-and-widget {
  display: flex;
}
.main-content {
  width: v-bind(mainContentWidth);
}
:deep(.feather-app-rail) {
  border-right: 0 !important;
}
</style>

<style lang="scss">
@use "@/styles/_app";
</style>

