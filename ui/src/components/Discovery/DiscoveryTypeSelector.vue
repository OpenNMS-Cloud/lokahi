<template>
  <div class="discovery-type-selector">
    <div class="flex-title">
      <FeatherBackButton @click="backButtonClick">Cancel</FeatherBackButton>
      <h2 class="title">{{ title }}</h2>
    </div>
    <p>Choose a discovery type to get started.</p>
    <div class="type-selectors">
      <h3>Active Discovery</h3>
      <p>Supporting copy that can quickly explain what an active discovery is.</p>
      <div
        @click="updateSelectedDiscovery('type', DiscoveryType.ICMP)"
        class="type-selector"
      >
        <div>Icon</div>
        <div>
          <h4>ICMP/SNMP</h4>
          <p>This could also contain additional supporting text to educate users.</p>
        </div>
      </div>
      <div
        @click="updateSelectedDiscovery('type', DiscoveryType.Azure)"
        class="type-selector"
      >
        <div>Icon</div>
        <div>
          <h4>Azure</h4>
          <p>This could also contain additional supporting text to educate users.</p>
        </div>
      </div>
    </div>

    <div class="type-selectors">
      <h3>Passive Discovery</h3>
      <p>Supporting copy that can quickly explain what a passive discovery is.</p>
      <div @click="updateSelectedDiscovery('type', DiscoveryType.SyslogSNMPTraps)">
        <div>Icon</div>
        <div>
          <h4>Syslogs and SNMP Traps</h4>
          <p>This could also contain additional supporting text to educate users.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DiscoveryType } from '@/components/Discovery/discovery.constants'

import Cloud from '@featherds/icon/action/Cloud'
import Network from '@featherds/icon/hardware/Network'
import { PropType } from 'vue'

defineProps({
  backButtonClick: { type: Function as PropType<() => void>, default: () => ({}) },
  updateSelectedDiscovery: { type: Function as PropType<(key: string, value: string) => void>, default: () => ({}) },
  title: { type: String, default: '' }
})
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@/styles/vars.scss';
@use '@/styles/mediaQueriesMixins';
</style>
