<template>
  <div class="header-container">
    <div class="welcome" v-if="keycloak?.authenticated" data-test="header-welcome">
        Welcome, {{ keycloak.tokenParsed?.preferred_username }}
    </div>
    <div class="btns">
      <AppliancesNotificationsCtrl />
      <AppliancesAddNodeCtrl />
    </div>
  </div>

  <div class="minions-devices-container">
    <div class="minions-table"><AppliancesMinionsTable /></div>
    <div class="devices-table"><AppliancesNodesTable /></div>
  </div>
</template>

<script lang="ts" setup>
import { useAppliancesStore } from '@/store/Views/appliancesStore'
import useKeycloak from '@/composables/useKeycloak'

const { keycloak } = useKeycloak()
const appliancesStore = useAppliancesStore()

const minionTableWidth = computed<string>(() => appliancesStore.minionsTableOpen ? '40%' : '0%')
const gapWidth = computed<string>(() => appliancesStore.minionsTableOpen ? '20px' : '0px')
</script>

<style scoped lang="scss">
@use "@featherds/styles/mixins/typography";

.header-container {
  display: flex;
  justify-content: space-between;
  margin: 1.5rem 1rem;
  .welcome {
    @include typography.headline2;
    font-weight: bold;
  }
  .btns {
    display: flex;
    justify-content: flex-end;
    gap: 20px;
  }
}
.minions-devices-container {
  display: flex;
  gap: v-bind(gapWidth);
  
  .minions-table {
    width: v-bind(minionTableWidth);
    transition: width 0.2s ease-out;
  }
  .devices-table {
    flex-grow: 1;
  }
}

// small screen / tablet / mobile
$breakpoint: 1024px;
@media (max-width: $breakpoint) {
  .header-container {
    display: block;
  }
  .minions-devices-container {
    display: block;
    .minions-table {
      width: 100%;
      margin-bottom: 20px;
    }
    .devices-table {
      width: 100%;
    }
  }
}
</style>
