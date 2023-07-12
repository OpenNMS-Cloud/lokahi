<template>
  <FeatherButton @click="inventoryStore.toggleTagManager" text>
    <template v-slot:icon>
      <FeatherIcon id="tagging" :icon="inventoryStore.isTagManagerOpen ? ExpandLess : ExpandMore" aria-hidden="true"
        focusable="false" flex />
      <label for="tagging" style="cursor:pointer;">Tag manager</label>
    </template>
  </FeatherButton>
</template>

<script setup lang="ts">
import ExpandMore from '@featherds/icon/navigation/ExpandMore'
import ExpandLess from '@featherds/icon/navigation/ExpandLess'
import { useInventoryStore } from '@/store/Views/inventoryStore'

const inventoryStore = useInventoryStore()

</script>

<style lang="scss" scoped>
@use '@/styles/btns.scss';

.btn-as-input {
  @include btns.btn-looks-as-select-input;
}
</style>
