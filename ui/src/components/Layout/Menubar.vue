<template>
  <FeatherAppHeader v-if="keycloak?.authenticated">
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

const { keycloak } = useKeycloak()
const { toggleDark } = useTheme()
</script>

<style lang="scss" scoped>
.btns {
  margin-right: 40px;
}
:deep(.header) {
  border-bottom: 0;
  justify-content: flex-end;
}
</style>
