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
        <FeatherAppRailNavItem href="/" :icon="Appliances" title="Appliances" :class="{ selected: isSelected('/') }" />
        <FeatherAppRailNavItem href="/map" :icon="Location" title="Map" :class="{ selected: isSelected('/map') }" />
        <FeatherAppRailNavItem href="/inventory" :icon="Business" title="Inventory" :class="{ selected: isSelected('/inventory') }" />
        <FeatherAppRailNavItem href="/discovery" :icon="Discovery" title="Discovery" :class="{ selected: isSelected('/discovery') }" />
        <FeatherAppRailNavItem href="/monitoring-policies" :icon="Monitoring" title="Monitoring Policies" :class="{ selected: isSelected('/monitoring-policies') }" />
      </template>
    </FeatherAppRail>
</template>

<script setup lang=ts>
import { IconTextAnimate, FeatherAppRailNavItem } from '@featherds/app-rail'
import Appliances from '@featherds/icon/hardware/Appliances'
import Location from '@featherds/icon/action/Location'
import Business from '@featherds/icon/action/Business'
import LogoIcon from '@/assets/OpenNMS-logo-icon.svg'
import LogoText from '@/assets/OpenNMS-logo-text.svg'
import Discovery from '@featherds/icon/action/Search'
import Monitoring from '@featherds/icon/hardware/MinionProfiles'

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