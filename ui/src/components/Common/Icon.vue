<!--
This component is used as a wrapper, over FeatherIcon component, to facilitating the use of `viewBox` attribute, which enables in setting dimension for an SVG.
  - viewBox: attribute is required to control an SVG icon dimension
    - @material-design-icons: does not have viewBox prop
    - required to set it manually on the FeatherIcon component with width/height
  - css: use font-size to set the icon dimension (recommended), with width and height set to 1em (already set
by FeatherIcon component)
  - svg: icon rendering props
    - @material-design-icons: only width/height available
    - @featherds: only viewBox available
 -->
<template>
  <FeatherTooltip
    :title="icon.tooltip || ''"
    v-slot="{ attrs, on }"
  >
    <FeatherIcon
      v-bind="icon.tooltip ? attrs : null"
      v-on="on"
      :icon="icon.image"
      :title="icon.title"
      :viewBox="setViewBox(icon.image)"
    />
  </FeatherTooltip>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import { setViewBox } from '@/components/utils'
import { IIcon } from '@/types'

const props = defineProps({
  icon: {
    type: Object as PropType<IIcon>,
    required: true
  }
})

const iconSize = `${props.icon.size || 1}rem` // FeatherIcon default width/height: 1rem
const cursorHover = props.icon.cursorHover || 'auto'
</script>

<style lang="scss" scoped>
svg.feather-icon {
  width: v-bind(iconSize);
  height: v-bind(iconSize);
  &:hover {
    cursor: v-bind(cursorHover);
  }
}
</style>
