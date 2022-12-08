<template>
  <FeatherIcon :icon="icon.image" :title="icon.title" :viewBox="setViewBox(icon.image)" />
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
