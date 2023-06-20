<template>
  <FeatherAppBar :labels="{ skip: 'main' }" content="app" v-if="keycloak?.authenticated">
    <template v-slot:right>
      <div class="right-container">
        <!-- All these are hidden for EAR -->
        <!-- <OptInOutCtrl /> -->
        <!-- <FeatherIcon
          :icon="LightDarkMode"
          class="pointer menu-icon"
          @click="toggleDark()"
          data-test="toggle-dark"
        />
        <FeatherIcon
          :icon="LogOut"
          class="pointer menu-icon"
          @click="logout()"
        /> -->
      </div>
    </template>
  </FeatherAppBar>
</template>
    
<script setup lang="ts">
import LightDarkMode from '@featherds/icon/action/LightDarkMode'
import LogOut from '@featherds/icon/action/LogOut'
import useKeycloak from '@/composables/useKeycloak'
import useTheme from '@/composables/useTheme'
import { logout } from '@/services/authService'

const { keycloak } = useKeycloak()
const { toggleDark } = useTheme()
</script>

<style lang="scss" scoped>
@use "@/styles/_app";

.menu-icon {
  font-size: 24px;
  margin-top: 2px;
  margin-right: 15px;
  &:last-child {
    margin-right: 0;
  }
}
:deep(.header) {
  border-bottom: 0;
}
</style>
  