<template>
  <div
    class="count-color"
    data-test="count-color"
  >
    <div>{{ count }}</div>
  </div>
</template>

<script lang="ts" setup>
defineProps({
  count: {
    type: Number,
    default: 0
  }
})
</script>

<style lang="scss" scoped>
.count-color {
  background-color: rgba(0, 102, 109, 0.12);
  color: #00666d;
  padding: 4px 8px;
  border-radius: 4px;
}
</style>
