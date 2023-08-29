<template>
  <div class="discovery-type-selector">
    <div class="flex-title">
      <FeatherBackButton @click="backButtonClick">Cancel</FeatherBackButton>
      <h2 class="title">{{ title }}</h2>
    </div>
    <p class="subtitle">
      Identify devices and entities to monitor through active or passive discovery. Choose a discovery type to get
      started.
    </p>
    <div
      class="type-selectors"
      v-for="discoveryTypeItem in discoveryTypeList"
      :key="discoveryTypeItem.title"
    >
      <div class="type-selector-header">
        <h3 class="type-selector-title">{{ discoveryTypeItem.title }}</h3>
        <p class="type-selector-subtitle">{{ discoveryTypeItem.subtitle }}</p>
      </div>
      <div
        v-for="discoveryType in discoveryTypeItem.discoveryTypes"
        class="type-selector"
        @click="() => updateSelectedDiscovery('type', discoveryType.value)"
        :key="discoveryType.value"
      >
        <div class="type-selector-row">
          <div class="type-selector-icon"><FeatherIcon :icon="discoveryType.icon" /></div>
          <div>
            <h4>{{ discoveryType.title }}</h4>
            <p>{{ discoveryType.subtitle }}</p>
          </div>
          <div class="type-selector-chevron"><FeatherIcon :icon="ChevronRight" /></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DiscoveryType } from '@/components/Discovery/discovery.constants'

import { PropType } from 'vue'
import AddNote from '@featherds/icon/action/AddNote'
import ChevronRight from '@featherds/icon/navigation/ChevronRight'

const discoveryTypeList = [
  {
    title: 'Active Discovery',
    subtitle: 'Query nodes and cloud APIs to detect the entities to monitor',
    discoveryTypes: [
      {
        title: 'ICMP/SNMP',
        subtitle: 'Perform a ping sweep and scan for SNMP MIBs on nodes that respond.',
        icon: AddNote,
        value: DiscoveryType.ICMP
      },
      {
        title: 'Azure',
        icon: AddNote,
        subtitle:
          'Connect to the Azure API, query the virtual machines list, and create entities for each VM in the node inventory.',
        value: DiscoveryType.Azure
      }
    ]
  },
  {
    title: 'Passive Discovery',
    subtitle: 'Use Syslog and SNMP traps to identify network devices. You can configure only one passive discovery.',
    discoveryTypes: [
      {
        title: 'Syslog and SNMP Traps',
        icon: AddNote,
        subtitle:
          "Identify devices through events, flows, and indirectly by evaluating other devices' configuration settings.",
        value: DiscoveryType.SyslogSNMPTraps
      }
    ]
  }
]

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

.type-selector-row {
  display: flex;
  align-items: center;
  padding: 20px;
  border-top: 1px solid var(--feather-border-on-surface);
  &:hover {
    background-color: var(--feather-background);
    cursor: pointer;
  }

  h4 {
    margin: 0;
    line-height: 1em;
  }
}
.type-selector-chevron {
  margin-left: auto;
  font-size: 22px;
}
.type-selector-header {
  padding: 20px;
}
.type-selectors {
  border: 1px solid var(--feather-border-on-surface);
  border-radius: var(--feather-border-radius-sm);
  margin-top: 20px;
  h1 {
    margin-top: 0;
  }
}
.type-selector-icon {
  margin-right: 18px;
  font-size: 22px;
}
.flex-title {
  display: flex;
  margin-bottom: 12px;
  h2 {
    margin-left: 25px;
  }
}
</style>
