<template>
  <div class="discoveries">
    <div class="discovery-card">
      <FeatherRadioGroup
        vertical
        :label="discoveryText.AddDiscoverySection.activeDiscoveryTitle"
        v-model="menuOption"
        @update:modelValue="setDiscoveryOption"
      >
        <div class="row">
          <FeatherIcon
            class="icon"
            :icon="networkIcon"
          />
          <FeatherRadio data-test="discoveryICMP" :value="DiscoveryType.ICMP">{{ discoveryText.AddDiscoverySection.icmpSnmp }}</FeatherRadio>
        </div>
        <div class="row">
          <FeatherIcon
            class="icon"
            :icon="cloudIcon"
          /><FeatherRadio data-test="discoveryAzure" :value="DiscoveryType.Azure">{{ discoveryText.AddDiscoverySection.azure }}</FeatherRadio>
        </div>
      </FeatherRadioGroup>
    </div>
    <div class="discovery-card passive">
      <FeatherRadioGroup
        horizontal
        :label="discoveryText.AddDiscoverySection.passiveDiscoveryTitle"
        v-model="menuOption"
        @update:modelValue="setDiscoveryOption"
      >
        <div class="row">
          <FeatherIcon
            class="icon"
            :icon="networkIcon"
          />
          <FeatherRadio data-test="discoverySyslog" :value="DiscoveryType.SyslogSNMPTraps">{{
            discoveryText.AddDiscoverySection.syslog
          }}</FeatherRadio>
        </div>
      </FeatherRadioGroup>
    </div>
  </div>
</template>

<script setup lang="ts">
import discoveryText from '@/components/Discovery/discovery.text'
import { DiscoveryType } from '@/components/Discovery/discovery.constants'

import Cloud from '@featherds/icon/action/Cloud'
import Network from '@featherds/icon/hardware/Network'
const cloudIcon = markRaw(Cloud)
const networkIcon = markRaw(Network)
const menuOption = ref(DiscoveryType.None)
const emit = defineEmits(['discovery-option-selected'])

const setDiscoveryOption = () => {
  emit('discovery-option-selected', menuOption.value)
}

const reset = () => {
  menuOption.value = DiscoveryType.None
}

defineExpose({
  reset
})
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@/styles/vars.scss';
@use '@/styles/mediaQueriesMixins';

.discoveries {
  padding: var(variables.$spacing-s);
  background: var(variables.$shade-4);
  display: flex;
  flex-direction: column;
  margin-top: var(variables.$spacing-xs);

  @include mediaQueriesMixins.screen-md {
    padding: var(variables.$spacing-l);
    display: flex;
    flex-direction: row;
  }
  .discovery-card {
    width: 100%;

    &.passive {
      border-left: none;
      padding-left: 0;
      border-top: 1px solid var(variables.$shade-4);
      padding-top: var(variables.$spacing-s);
      margin-top: var(variables.$spacing-s);
      .icon {
        display: none;
      }

      @include mediaQueriesMixins.screen-md {
        border-top: none;
        border-left: 1px solid var(variables.$shade-4);
        padding-left: var(variables.$spacing-l);
        padding-top: 0;
        margin-top: 0;
        .icon {
          display: flex;
        }
      }
    }

    @include mediaQueriesMixins.screen-md {
      width: 50%;
    }
  }
  .row {
    display: flex;
    align-items: center;
  }
  .icon {
    width: 28px;
    height: 28px;
    color: var(variables.$shade-2);
    margin-right: var(variables.$spacing-l);
  }
}

.layout-container {
  margin: 0 !important;
}
:deep(.feather-radio-group-container label) {
  margin-bottom: var(variables.$spacing-xs) !important;
}

:deep(.feather-input-sub-text) {
  display: none;
}
</style>
