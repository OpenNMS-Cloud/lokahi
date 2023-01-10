<template>
  <CustomFeatherStepper>
    <DiscoveryStep1 />
    <DiscoveryStep2 />
    <DiscoveryStep3 nextBtnText="Exit" @slideNext="closeModal" />
  </CustomFeatherStepper>
</template>

<script setup lang="ts">
import useModal from '@/composables/useModal'
const { closeModal } = useModal()
</script>
