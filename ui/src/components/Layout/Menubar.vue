<template>
  <FeatherAppHeader v-if="keycloak?.authenticated">
    <div class="app-header-title">
      {{ title }}
    </div>
    <div class="btns">
      <!-- Hidden until ready -->
      <!-- <OptInOutCtrl /> -->
      <FeatherButton
        data-test="toggle-dark"
        icon="Light/Dark Mode"
        @click="toggleDark()"
      >
        <FeatherIcon :icon="LightDarkMode" />
      </FeatherButton>

      <FeatherButton
        @click="keycloak.logout()"
        icon="Logout"
      >
        <FeatherIcon :icon="LogOut" />
      </FeatherButton>
    </div>
  </FeatherAppHeader>
</template>

<script setup lang="ts">
import LightDarkMode from '@featherds/icon/action/LightDarkMode'
import LogOut from '@featherds/icon/action/LogOut'
import useKeycloak from '@/composables/useKeycloak'
import useTheme from '@/composables/useTheme'
import { useAppStore } from '@/store/Views/appStore'

const { keycloak } = useKeycloak()
const { toggleDark } = useTheme()
const appStore = useAppStore()

const title = computed(() => appStore.isCloud ? 'OpenNMS CLOUD' : 'Lōkahi')
</script>

<style lang="scss" scoped>
@use '@featherds/styles/mixins/typography';
.btns {
  margin-right: 40px;
}
.app-header-title {
  @include typography.caption;
  font-size: 14px;
  margin-left: 95px;
}
:deep(.header) {
  border-bottom: 0;
  justify-content: space-between;
}
</style>
