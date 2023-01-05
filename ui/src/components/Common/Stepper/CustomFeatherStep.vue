<template>
  <slot></slot>
</template>

<script setup lang="ts">
defineEmits([
  'slideNext', 
  'slidePrev'
])

defineProps({
  nextBtnText: String,
  prevBtnText: String
})
</script>
