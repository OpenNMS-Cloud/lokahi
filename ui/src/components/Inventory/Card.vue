<template>
  <h4>{{ item.header }}</h4>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'
import { NodeDetail } from '@/types/inventory'

defineProps({
  item: {
    type: Object as PropType<NodeDetail>,
    required: true
  }
})
</script>

<style lang="scss" scoped></style>