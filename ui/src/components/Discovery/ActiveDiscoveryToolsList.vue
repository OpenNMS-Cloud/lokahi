<template>
  <div class="section type-tool">
    <FeatherIcon
      class="icon"
      :icon="Network"
    />
    <div
      class="tool"
      @click="emit('show-config-active-tool', DiscoveryType.ICMP)"
    >
      ICMP
    </div>
  </div>
  <div class="type-tool">
    <FeatherIcon
      class="icon"
      :icon="Cloud"
    />
    <div
      class="tool"
      @click="emit('show-config-active-tool', DiscoveryType.Azure)"
    >
      AZURE
    </div>
  </div>
</template>

<script setup lang="ts">
import Cloud from '@featherds/icon/action/Cloud'
import Network from '@featherds/icon/hardware/Network'
import { DiscoveryType } from './discovery.constants'
const emit = defineEmits(['show-config-active-tool'])
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';

.icon {
    border: 1px solid #858585;
    border-radius: 20px;
    padding: 5px;
    width: 34px;
    height: 34px;
    color: #858585;
}

.section {
    margin-bottom: 14px;
    padding: 10px 0;

    border-bottom: 1px solid #d8d8d8;
}

.type-tool {
    display: flex;
    align-items: center;
    .tool {
        background-color: gray;
        color: #ffffff;
        border-radius: 25px;
        padding: 0 15px;
        font-size: 14px;
        font-weight: 600;
        margin-left: 25px;
        cursor: pointer;
    }
}
</style>
