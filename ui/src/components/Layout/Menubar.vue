<template>
  <FeatherAppBar :labels="{ skip: 'main' }" content="app" v-if="keycloak?.authenticated">
    <template v-slot:right>
      <FeatherIcon
        :icon="Dashboard"
        class="pointer menu-icon widgets"
        @click="triggerWidgetBar()"
      />
      <FeatherIcon
        :icon="LightDarkMode"
        class="pointer menu-icon"
        @click="toggleDark()"
        data-test="toggle-dark"
      />
      <FeatherIcon
        :icon="LogOut"
        class="pointer menu-icon"
        @click="logout()"
      />
    </template>
  </FeatherAppBar>
</template>
    
<script setup lang="ts">
import LightDarkMode from '@featherds/icon/action/LightDarkMode'
import LogOut from '@featherds/icon/action/LogOut'
import Dashboard from '@featherds/icon/action/Dashboard'
import useKeycloak from '@/composables/useKeycloak'
import useTheme from '@/composables/useTheme'
import { logout } from '@/services/authService'
import { useLayoutStore } from '@/store/Views/layoutStore'

const { keycloak } = useKeycloak()
const { toggleDark } = useTheme()
const { triggerWidgetBar } = useLayoutStore()
</script>

<style lang="scss">
@import "@featherds/styles/themes/open-mixins";
body {
  background: var($background);
}
.open-light {
  @include open-light;
}
.open-dark {
  @include open-dark;
}
.menu-icon {
  font-size: 24px;
  margin-top: 2px;
  margin-right: 15px;
  &:last-child {
    margin-right: 0;
  }
}
</style>
  