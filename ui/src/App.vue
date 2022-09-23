  
<template>
  <FeatherAppLayout contentLayout="full" class="feather-styles layout">
    <template v-slot:header>
      <Menubar />
    </template>

    <template v-slot:rail>
      <NavigationRail :modelValue="store.navRailOpen" />
    </template>

    <div class="content-and-widget">
      <div class="main-content">
        <Spinner />
        <Snackbar />
        <router-view />
      </div>
      <transition name="fade">
        <Widgetbar v-if="store.widgetBarOpen" />
      </transition>
    </div>

  </FeatherAppLayout>
</template>
  
<script setup lang="ts">
import { useLayoutStore } from './store/Views/layoutStore'

// Remove KC redirectUri theme param
const route = useRoute()
const router = useRouter()
if (route.query.theme) router.replace(route.path)

// transition nav rail open / close
const store = useLayoutStore()
const contentMargin = computed(() => store.navRailOpen ? '230px' : '15px')
const ease = computed(() => store.navRailOpen ? '10ms' : '80ms')
const maxWidth = computed(() => {
  const navWidth = store.navRailOpen ? 223 : 0
  const widgetWidth = store.widgetBarOpen ? 500 : 0
  return navWidth + widgetWidth + 'px'
})
</script>
  
<style lang="scss">
@import "@featherds/styles/lib/grid";
@import "@featherds/styles/mixins/typography";
@import "@featherds/styles/themes/open-mixins";
@import "@featherds/table/scss/table";
@import "@featherds/styles/themes/variables";

html {
  overflow-x: hidden;
}

body {
  margin: 0
}

.content-and-widget {
  display: flex;
}

.main-content {
  position: relative;
  width: 100%;
  margin-left: v-bind(contentMargin);
  margin-right: 12px;
  transition: margin-left 0.28s ease-in-out v-bind(ease);
  max-width: calc(100% - v-bind(maxWidth));

  table {
    width: 100%;
    @include table;
  }

  .data-table tr,
  .data-table div {
    @for $i from 1 through 50 {
    &:nth-child(#{$i}) {
      transition: all 0.3s ease $i * 0.05s;
    }
    }
  }
  .data-table-enter-active,
  .data-table-leave-active {
    transform: translateX(0px);
    opacity:1;
  }
  .data-table-enter-from,
  .data-table-leave-to {
    transform: translateX(30px);
    opacity:0;
  }
}

a {
  text-decoration: none;
  color: var($primary);
}

.pointer {
  cursor: pointer;
}

.bg-ok {
  background-color: var($success);
  color: var($primary-text-on-color);
}
.bg-failed {
  background-color: var($error);
  color: var($primary-text-on-color);
}
.bg-unknown {
    background-color: grey;
    color: var($primary-text-on-color);
}
.open-dark {
  .bg-unknown {
    color: rgb(10, 12, 27);
  }
}
</style>

<style scoped lang="scss">
  .fade-enter-active {
    transition: all 0.5s;
  }
  .fade-enter-from,
  .fade-leave-to {
    opacity: 0;
  }
</style>
