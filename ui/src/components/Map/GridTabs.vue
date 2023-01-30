<template>
  <FeatherTabContainer class="tabs">
    <template v-slot:tabs>
      <FeatherTab ref="alarmTab" @click="goToAlarms" data-test="alarm-tab">Alarms({{ alarms?.length }})</FeatherTab>
      <FeatherTab ref="nodesTab" @click="goToNodes" data-test="nodes-tab">Nodes({{ nodes?.length }})</FeatherTab>
    </template>
  </FeatherTabContainer>
  <router-view />
</template>
<script setup lang="ts">
import { useMapStore } from '@/store/Views/mapStore'

const mapStore = useMapStore()
const router = useRouter()
const route = useRoute()
const nodes = computed(() => mapStore.devicesInbounds)
const alarms = computed<any[]>(() => mapStore.alarms)
const alarmTab = ref()
const nodesTab = ref()

const goToAlarms = () => router.push(`/map${route.query.nodeid ? '?nodeid=' + route.query.nodeid : ''}`)

const goToNodes = () => router.push('/map/nodes')

/**
 * onActivated hook was used in classic bc of MarkerCluster package bug occurred when remounting the map.
 * It could happen when we add more functionality to the map.
 * 
 * <router-view v-slot="{ Component }">
        <keep-alive include="MapKeepAlive">
          <component :is="Component" />
        </keep-alive>
      </router-view>
*/
/* onActivated(() => {
  if (router.currentRoute.value.name === 'MapAlarms') {
    alarmTab.value.tab.click()
  } else {
    nodesTab.value.tab.click()
  }
}) */
onMounted(async () => {
  const tabRef = router.currentRoute.value.name === 'MapAlarms' ? alarmTab : nodesTab
  await tabRef.value.tab.click()
  tabRef.value.tab.classList.remove('focus')
})
</script>

<style scoped lang="scss">
@use "@featherds/styles/themes/variables";

.tabs {
  z-index: 2;
  padding-bottom: 10px;
  margin-bottom: -29px;
  background: var(variables.$surface);
}
</style>
