<template>
  <div class="azure-container">
    <div>
      <div class="location-title">Select a location</div>
      <DiscoveryLocations single />
    </div>
    <div class="azure-form">
      <FeatherInput
        v-model="store.azure.clientId"
        label="ClientID"
      >
      </FeatherInput>
      <FeatherProtectedInput
        v-model="store.azure.clientSecret"
        label="Client Secret"
      />
      <FeatherInput
        v-model="store.azure.subscriptionId"
        label="SubscriptionID"
      >
      </FeatherInput>
      <FeatherInput
        v-model="store.azure.directoryId"
        label="DirectoryID"
      >
      </FeatherInput>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
const store = useDiscoveryStore()
</script>

<style scoped lang="scss">
@use "@featherds/styles/themes/variables";
.azure-container {
  min-width: 800px;

  > .azure-form {
    display: flex;
    flex-wrap: wrap;
    width: 100%;
    justify-content: space-between;
    border-top: 1px solid var(variables.$shade-4);
    padding-top: var(variables.$spacing-l);
    margin-top: var(variables.$spacing-s);
    > div {
        width: 49%;
    }
  }

  .location-title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: -24px;
  }
}
</style>
