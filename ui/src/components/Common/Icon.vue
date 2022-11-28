<template>
    <FeatherIcon :icon="item.icon" :title="item.title" :class="item.classes" :viewBox="setViewBox(item.icon)" />
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import { setViewBox } from '@/components/utils'

interface Item {
  icon: any,
  title?: string,
  classes?: string,
  color?: string,
  colorHover?: string
}

defineProps({
  item: {
    type: Object as PropType<Item>,
    required: true
  }
})
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";

.feather-icon {
  font-size: 1.5rem;
  color: var(--feather-primary-text-on-surface);
}
</style>