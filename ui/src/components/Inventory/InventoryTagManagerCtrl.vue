<template>
  <FeatherButton
    @click="toggleTagManager"
    class="btn-as-input"
  >
    <template v-slot:icon>
      <label for="tagging">Tag manager</label>
      <FeatherIcon
        id="tagging"
        :icon="isTagManagerOpen ? icon.ExpandLess : icon.ExpandMore"
        aria-hidden="true"
        focusable="false"
        flex
      />
    </template>
  </FeatherButton>
</template>

<script setup lang="ts">
import ExpandMore from '@featherds/icon/navigation/ExpandMore'
import ExpandLess from '@featherds/icon/navigation/ExpandLess'
import { useInventoryStore } from '@/store/Views/inventoryStore'

const inventoryStore = useInventoryStore()

const isTagManagerOpen = computed(() => inventoryStore.isTagManagerOpen)
const toggleTagManager = () => {
  inventoryStore.toggleTagManager()
}

const icon = markRaw({ ExpandLess, ExpandMore })
</script>

<style lang="scss" scoped>
@use '@/styles/btns.scss';

.btn-as-input {
  @include btns.btn-looks-as-select-input;
}
</style>
