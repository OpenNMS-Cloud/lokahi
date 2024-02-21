<template>
  <FeatherListSwitch
    class="basic-switch"
    v-model="toggled"
    @update:modelValue="$emit('toggle', toggled)"
  />
</template>

<script lang="ts" setup>
const toggled = ref(false)

const props = defineProps<{
  toggle?: boolean
}>()

watchEffect(() => {
  if (props.toggle !== undefined) {
    toggled.value = props.toggle
  }
})
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';

:deep(.feather-list-item:hover) {
  background: var(variables.$surface) !important;
}
:deep(.ripple) {
  display: none !important;
}
</style>
