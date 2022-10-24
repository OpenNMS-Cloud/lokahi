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
        <FeatherAppRailNavItem href="/" :icon="Server" title="Appliances" :class="{ selected: isSelected('/') }" />
        <FeatherAppRailNavItem href="/map" :icon="Pin" title="Map" :class="{ selected: isSelected('/map') }" />
        <FeatherAppRailNavItem href="/graphs" :icon="Graphs" title="Graphs" :class="{ selected: isSelected('/graphs') }" />
      </template>
    </FeatherAppRail>
</template>

<script setup lang=ts>
import { IconTextAnimate, FeatherAppRailNavItem } from '@featherds/app-rail'
import Server from '@featherds/icon/hardware/Server'
import Pin from '@featherds/icon/action/Pin'
import Graphs from '@featherds/icon/action/Reporting'
import LogoIcon from '@/assets/OpenNMS-logo-icon.svg'
import LogoText from '@/assets/OpenNMS-logo-text.svg'

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
@import "@featherds/styles/mixins/typography";

.icon-text-animate {
  &:hover {
    cursor: pointer;
  }
}
.product-text {
  @include headline2;
  color: var(--feather-app-rail-text-color);
}
</style>