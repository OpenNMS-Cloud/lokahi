<template>
  <FeatherAppRail
    :labels="labels"
    :content="content"
  >
    <template v-slot:icon>
      <router-link to="/">
        <IconTextAnimate class="nav-rail-home">
          <template v-slot:icon>
            <FeatherIcon
              :icon="LogoIcon"
              title="Home"
            />
          </template>
          <template v-slot:text>
            <FeatherIcon
              :icon="LogoText"
              title="Home"
            />
          </template>
        </IconTextAnimate>
      </router-link>
    </template>
    <template v-slot:nav>
      <NavigationRailNavItem
        title="Home"
        href="/"
        :icon="Icons.Home"
      />

      <NavigationRailNavItem
        title="Appliances"
        href="/appliances"
        :icon="Icons.Appliances"
      />

      <NavigationRailNavItem
        title="Map"
        href="/map"
        :icon="Icons.Location"
      />

      <NavigationRailNavItem
        title="Inventory"
        href="/inventory"
        :icon="Icons.Business"
      />

      <NavigationRailNavItem
        title="Discovery"
        href="/discovery"
        :icon="Icons.Discovery"
      />

      <NavigationRailNavItem
        title="Monitoring Policies"
        href="/monitoring-policies"
        :icon="Icons.Monitoring"
      />

      <NavigationRailNavItem
        title="Alerts"
        href="/alerts"
        :icon="Icons.Warning"
      />

      <NavigationRailNavItem
        title="Locations"
        href="/locations"
        :icon="Icons.Location"
      />

      <NavigationRailNavItem
        v-if="false"
        title="Synthetic Transactions"
        href="/synthetic-transactions"
        :icon="Icons.Cycle"
      />
    </template>
  </FeatherAppRail>
</template>

<script setup lang="ts">
import { IconTextAnimate, FeatherAppRail } from '@featherds/app-rail'

import Appliances from '@featherds/icon/hardware/Appliances'
import Home from '@featherds/icon/action/Home'
// import Map from '@material-design-icons/svg/outlined/map.svg'
import Business from '@featherds/icon/action/Business'
import LogoIcon from '@/assets/OpenNMS-logo-icon.svg'
import LogoText from '@/assets/OpenNMS-logo-text.svg'
import Discovery from '@featherds/icon/action/Search'
import Monitoring from '@featherds/icon/hardware/MinionProfiles'
import Cycle from '@featherds/icon/action/Cycle'
import Warning from '@featherds/icon/notification/Warning'
import Location from '@featherds/icon/action/Location'
import NavigationRailNavItem from '@/components/Layout/NavigationRailNavItem.vue'

const Icons = markRaw({
  Appliances,
  Home,
  // Map,
  Business,
  Discovery,
  Monitoring,
  Cycle,
  Warning,
  Location
})

const labels = {
  skip: 'Skip to main content'
}
const content = 'mainContent'
</script>
