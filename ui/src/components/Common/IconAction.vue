<template>
  <component :is="asLi ? 'li' : 'a'" :title="item.title" class="pointer">
    <FeatherIcon @click="item.action" :icon="item.icon" :title="item.title" :viewBox="viewBox" />
  </component>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import { fncVoid } from '@/types'

interface Item {
  title?: string,
  icon: any,
  action: fncVoid
}

const props = defineProps({
  item: {
    type: Object as PropType<Item>,
    required: true
  },
  asLi: {
    type: Boolean,
    default: false
  }
})

/* Note: 
    - viewBox: attribute is required to control the icon dimension
      - @material-design-icons: does not have viewBox prop - need to set it manually on the FeatherIcon component with width/height
    - css: use font-size to set the icon dimension (recommended), with width and height set to 1em (already set by FeatherIcon component)
    - svg: icon rendering props
      - @material-design-icons: only width/height available
      - @featherds: only viewBox available
 */
const iconProps = props.item.icon.render().props
const viewBox = iconProps.viewBox || `0 0 ${iconProps.width} ${iconProps.height}`
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";

.feather-icon {
  font-size: 1.5rem;
  color: var(variables.$disabled-text-on-surface);
  &:hover {
    color: var(variables.$primary-text-on-surface);
  }
}
</style>