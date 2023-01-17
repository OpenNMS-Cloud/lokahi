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

defineProps({
  icon: {
    type: Object as PropType<IIcon>,
    required: true
  }
})
</script>
