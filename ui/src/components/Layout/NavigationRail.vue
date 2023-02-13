<template>
  <FeatherAppRail :labels="labels" :content="content">
      <template v-slot:icon>
        <IconTextAnimate class="nav-rail-home">
          <template v-slot:icon>
            <FeatherIcon
              :icon="LogoIcon"
              title="Home"
              class="product-icon"
              @click="gotoHome" />
          </template>
          <template v-slot:text>
            <FeatherIcon
              :icon="LogoText"
              title="Home"
              class="product-icon"
              @click="gotoHome" />
          </template>
        </IconTextAnimate>
      </template>
      <template v-slot:nav>
        <!-- hide dashboard link until complete -->
        <FeatherAppRailNavItem v-if="false" href="/dashboard" :icon="Icons.Dashboard" title="Dashboard" :class="{ selected: isSelected('/dashboard') }" />
        <FeatherAppRailNavItem href="/" :icon="Icons.Appliances" title="Appliances" :class="{ selected: isSelected('/') }" />
        <FeatherAppRailNavItem href="/map" :icon="Icons.Location" title="Map" :class="{ selected: isSelected('/map') }" />
        <FeatherAppRailNavItem href="/inventory" :icon="Icons.Business" title="Inventory" :class="{ selected: isSelected('/inventory') }" />
        <FeatherAppRailNavItem href="/discovery" :icon="Icons.Discovery" title="Discovery" :class="{ selected: isSelected('/discovery') }" />
        <FeatherAppRailNavItem href="/monitoring-policies" :icon="Icons.Monitoring" title="Monitoring Policies" :class="{ selected: isSelected('/monitoring-policies') }" />
        <FeatherAppRailNavItem v-if="false" href="/synthetic-transactions" :icon="Icons.Cycle" title="Synthetic Transactions" :class="{ selected: isSelected('/synthetic-transactions') }" />
      </template>
    </FeatherAppRail>
</template>

<script setup lang=ts>
import { IconTextAnimate, FeatherAppRailNavItem } from '@featherds/app-rail'
import Appliances from '@featherds/icon/hardware/Appliances'
import Dashboard from '@featherds/icon/action/Dashboard'
import Location from '@featherds/icon/action/Location'
import Business from '@featherds/icon/action/Business'
import LogoIcon from '@/assets/OpenNMS-logo-icon.svg'
import LogoText from '@/assets/OpenNMS-logo-text.svg'
import Discovery from '@featherds/icon/action/Search'
import Monitoring from '@featherds/icon/hardware/MinionProfiles'
import Cycle from '@featherds/icon/action/Cycle'

const Icons = markRaw({
  Appliances,
  Dashboard,
  Location,
  Business,
  Discovery,
  Monitoring,
  Cycle
})

const labels = {
  skip: 'Skip to main content'
}
const content = 'mainContent'

const router = useRouter()
const route = useRoute()

const isSelected = (path: string) => path === route.fullPath

const gotoHome = () => {
  router.push('/')
}
</script>

<style lang="scss" scoped>
@use "@featherds/styles/mixins/typography";

.icon-text-animate {
  &:hover {
    cursor: pointer;
  }
}
.product-text {
  @include typography.headline2;
  color: var(--feather-app-rail-text-color);
}
</style>