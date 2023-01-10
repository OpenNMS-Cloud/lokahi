<template>
  <CustomFeatherStep title="Done!">
    <div class="step-3-container">
      <div>
        <FeatherList>
          <FeatherListHeader>IP Addresses</FeatherListHeader>
          <FeatherListItem v-for="ipAddress of store.ipAddresses">
            {{ ipAddress }}
          </FeatherListItem>
        </FeatherList>
      </div>
      <div>
        <FeatherList>
          <FeatherListHeader>IP Range</FeatherListHeader>
          <FeatherListItem v-if="store.ipRange.cidr">
            {{ store.ipRange.cidr }} {{ store.ipRange.fromIp }} - {{ store.ipRange.toIp }}
          </FeatherListItem>
        </FeatherList>
      </div>
      <div class="step-3-text-container">
        <a class="step-3-text" href="inventory">Last detected nodes</a>
        <a class="step-3-text" href="my-discovery">My Active Discovery</a>
      </div>
    </div>
  </CustomFeatherStep>
</template>

<script setup lang="ts">
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
const store = useDiscoveryStore()
</script>

<style scoped lang="scss">
@use "@featherds/styles/themes/variables";
@use "@featherds/styles/mixins/typography";

.step-3-container {
  display: flex;
  justify-content: space-around;

  .step-3-text-container {
    margin-top: var(variables.$spacing-m);
    .step-3-text {
      @include typography.subtitle1;
      color: var(variables.$secondary-variant);
      cursor: pointer;
      display: block;
      text-decoration: none;
    }
  }
}
</style>
