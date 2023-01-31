<template>
  <FeatherButton
    @click="taggingBoxToggle"
    class="btn-as-input"
    name="tagging"
  >
    <template v-slot:icon>
      <label for="tagging">Tagging</label>
      <FeatherIcon
        id="tagging"
        :icon="taggingBoxIsOpen ? upIcon : downIcon"
        aria-hidden="true"
        focusable="false"
        flex
      />
    </template>
  </FeatherButton>
</template>

<script setup lang="ts">
import { useInventoryStore } from '@/store/Views/inventoryStore'
import ArrowDown from '@featherds/icon/navigation/ExpandMore'
import ArrowUp from '@featherds/icon/navigation/ExpandLess'

const inventoryStore = useInventoryStore()

const taggingBoxIsOpen = computed(() => inventoryStore.isTaggingBoxOpen)
const taggingBoxToggle = () => inventoryStore.toggleTaggingBox()

const upIcon = markRaw(ArrowUp)
const downIcon = markRaw(ArrowDown)
</script>

<style lang="scss" scoped>
@use '@/styles/btns.scss';

.btn-as-input {
  @include btns.btn-looks-as-select-input;
}
</style>
