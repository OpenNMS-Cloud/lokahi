<template>
  <FeatherDialog v-model="modalState" :labels="labels" relative>
    <div class="flex">
      <div v-for="(value, key) in ICON_PATHS" :key="key">
        <img
          width="80"
          :src="value"
          class="pointer icon"
          @click="selectIcon(key)"
          :class="{ 'selected-icon': key === selectedIconKey }"
          :alt="key"
        />
      </div>
    </div>

    <template v-slot:footer>
      <FeatherButton secondary @click="changeIcon">Ok</FeatherButton>
      <FeatherButton secondary @click="close">Cancel</FeatherButton>
    </template>
  </FeatherDialog>
</template>
<script setup lang=ts>
import { useTopologyStore } from '@/store/Views/topologyStore'
import { FeatherDialog } from '@featherds/dialog'
import { FeatherButton } from '@featherds/button'
import ICON_PATHS from './icons/iconPaths'

const props = defineProps({
  nodeId: {
    type: String,
    required: true
  }
})

const topologyStore = useTopologyStore()
const selectedIconKey = ref()
const { nodeId } = toRefs(props)

const modalState = computed({
  get: () => topologyStore.modalState,
  set: (value: boolean) => topologyStore.modalState = value
})

const labels = {
  title: 'Change Icon',
  close: 'Close'
}

const selectIcon = (key: string) => selectedIconKey.value = key
const changeIcon = () => {
  topologyStore.changeIcon({ [nodeId.value]: selectedIconKey.value })
  close()
}
const close = () => modalState.value = false
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";

.dialog {
  min-width: 500px;
  height: 300px;
}
.flex {
  display: flex;
  flex-wrap: wrap;
  max-width: 700px;
  max-height: 400px;
  overflow: auto;

  .icon {
    margin: 5px;
  }

  .selected-icon {
    box-sizing: border-box;
    border: 2px solid var(variables.$primary);
  }
}
</style>
