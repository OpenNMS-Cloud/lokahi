<template>
  <div class="links">
    <FeatherDropdown>
      <template v-slot:trigger="{ attrs, on }">
        <FeatherButton
          link
          href="#"
          v-bind="attrs"
          v-on="on"
          primary
          menu-trigger
          data-test="menu-dropdown"
        >
          <div class="dropdown-title">{{ dashboardText.MenuLabel }}</div>
          <FeatherIcon
            :icon="Menu"
            class="icon-menu"
          />
        </FeatherButton>
      </template>
      <FeatherDropdownItem
        :key="action.link"
        data-test="menu-links"
        v-for="action in dashboardText.MenuLinks"
        @click="redirect(action.link)"
        >{{ action.name }}</FeatherDropdownItem
      >
    </FeatherDropdown>
  </div>
</template>

<script setup lang="ts">
import Menu from '@featherds/icon/navigation/Menu'
import dashboardText from '@/components/Dashboard/dashboard.text'

const router = useRouter()
const redirect = (route: string) => {
  router.push({ name: route })
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/themes/variables';
@use '@/styles/mediaQueriesMixins.scss';

.links {
  .dropdown-title {
    display: none;

    @include mediaQueriesMixins.screen-md {
      display: block;
    }
  }
  .icon-menu {
    font-size: 26px;
    display: flex;
    @include mediaQueriesMixins.screen-md {
      display: none;
    }
  }
}
</style>
