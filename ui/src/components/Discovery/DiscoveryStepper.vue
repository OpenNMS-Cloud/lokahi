<template>
  {{  submitted }}
  err: {{  error }}
  <CustomFeatherStepper class="discovery-stepper">
    <DiscoveryStep1 :disableNextBtn="!store.selectedLocations.length" />
    <DiscoveryStep2 :disableNextBtn="step2Disabled" @slideNext="step2Submit"/>
    <DiscoveryStep3 :hideNextBtn="true" @slideNext="callback" />
  </CustomFeatherStepper>
</template>

<script setup lang="ts">
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
import useValidation from './useValidation'

const { setSubmitted, error, submitted } = useValidation()
const store = useDiscoveryStore()
defineProps<{
  callback: () => void
}>()

const step2Disabled = computed(() => {
  return Boolean(!store.ipAddresses.length) && 
  (!store.ipRange.cidr || !store.ipRange.fromIp || !store.ipRange.toIp)
})

const step2Submit = (event: Event) => {
  setSubmitted()
  if (error.value) {
    event.stopPropagation()
  }
}
</script>

<style scoped lang="scss">
@use "@featherds/styles/themes/variables";
.discovery-stepper {
  min-width: 650px;
  padding: var(variables.$spacing-xl);
  border-bottom: 1px solid var(variables.$shade-4);
}
</style>
