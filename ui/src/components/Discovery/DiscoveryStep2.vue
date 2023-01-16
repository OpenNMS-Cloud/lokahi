<template>
  <CustomFeatherStep>
    <div class="ip-address-container">
      <div class="title">Specific IPs</div>
      <FeatherInput
        label="IP Addresses"
        v-model="ipAddressesStr"
        hint="ex: 172.16.32.1, 172.16.32.2 ..."
        @update:modelValue="updateIpsPayload"
      />
    </div>

    <div class="ip-range-container">
      <div class="title">IP Range</div>
      <div class="input-container">
        <FeatherInput
          label="CIDR"
          v-model="store.ipRange.cidr"
          hint="ex: 172.16.0.0/16"
          class="input"
        />
        <FeatherInput
          label="From"
          v-model="store.ipRange.fromIp"
          hint="ex: 172.16.0.0"
          class="input"
        />      
        <FeatherInput
          label="To"
          v-model="store.ipRange.toIp"
          hint="ex: 172.16.255.255"
          class="input"
        />
      </div>
    </div>
  </CustomFeatherStep>
</template>

<script setup lang="ts">
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
const store = useDiscoveryStore()
const ipAddressesStr = ref<string>('')
const updateIpsPayload = () => {
  if (ipAddressesStr.value) {
    store.ipAddresses = ipAddressesStr.value.split(',')
  } else {
    store.ipAddresses = []
  }
}
onBeforeMount(() => {
  // populate ipAddresses field with previous data
  if (store.ipAddresses.length) {
    ipAddressesStr.value = store.ipAddresses.join(', ')
  }
})
</script>

<style scoped lang="scss">
@use "@featherds/styles/themes/variables";
@use "@featherds/styles/mixins/typography";

.title {
  @include typography.subtitle1;
  margin-bottom: var(variables.$spacing-s);
}
.ip-range-container {
  display: flex;
  flex-direction: column;
  margin-top: var(variables.$spacing-m);

  .input-container {
    display: flex;
    gap: var(variables.$spacing-s);

    .input {
      flex-basis: 200px;
      flex-grow: 1;
    }
  }
}
</style>
