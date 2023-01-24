<template>
  <CustomFeatherStepper ref="stepper" class="discovery-stepper">
    <DiscoveryStep1 :disableNextBtn="!store.selectedLocations.length" />
    <DiscoveryStep2 :disableNextBtn="step2Disabled" @slideNext="step2Submit"/>
    <DiscoveryStep3 :hideNextBtn="true" />
  </CustomFeatherStepper>
</template>

<script setup lang="ts">
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
import useDiscoveryValidation from './useDiscoveryValidation'

const { validate, error } = useDiscoveryValidation()
const store = useDiscoveryStore()
const stepper = ref()

const step2Disabled = computed(() => {
  return Boolean(!store.ipAddresses.length) && 
  (!store.ipRange.cidr || !store.ipRange.fromIp || !store.ipRange.toIp)
})

const step2Submit = () => {
  validate()
  if (!error.value) {
    stepper.value.next()
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
