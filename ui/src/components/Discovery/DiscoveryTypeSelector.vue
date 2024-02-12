<template>
  <div class="discovery-type-selector">
    <div class="flex-title">
      <h2 class="title">{{ title }}</h2>
    </div>
    <p class="subtitle">
      Identify devices and entities to monitor through
      <FeatherPopover
        :pointer-alignment="PointerAlignment.center"
        :placement="PopoverPlacement.bottom"
      >
        <template #default>
          <div>
            <h4>Active Discovery</h4>
            <p>Active discovery queries nodes and cloud APIs to detect the entities that you want to monitor.</p>
            <a
              @click="
                () => {
                  discoveryStore.activateHelp(InstructionsType.Active)
                }
              "
              class="full"
              >Read full article
              <FeatherIcon :icon="ChevronRight" />
            </a>
          </div>
        </template>
        <template #trigger="{ attrs, on }">
          <span
            class="pop"
            v-bind="attrs"
            v-on="on"
            >active</span
          >
        </template>
      </FeatherPopover>
      or
      <FeatherPopover
        :pointer-alignment="PointerAlignment.center"
        :placement="PopoverPlacement.bottom"
      >
        <template #default>
          <div>
            <h4>Passive Discovery</h4>
            <p>Passive discovery uses Syslog and SNMP traps to identify network devices.</p>
            <a
              @click="
                () => {
                  discoveryStore.activateHelp(InstructionsType.Passive)
                }
              "
              class="full"
              >Read full article
              <FeatherIcon :icon="ChevronRight" />
            </a>
          </div>
        </template>
        <template #trigger="{ attrs, on }">
          <span
            class="pop"
            v-bind="attrs"
            v-on="on"
            >passive</span
          >
        </template>
      </FeatherPopover>
      discovery. <br />
      Choose a discovery type to get started.
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
            <h5>{{ discoveryType.title }}</h5>
            <p class="t-subtitle">{{ discoveryType.subtitle }}</p>
          </div>
          <div class="type-selector-chevron"><FeatherIcon :icon="ChevronRight" /></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DiscoveryType, InstructionsType } from '@/components/Discovery/discovery.constants'

import ChevronRight from '@featherds/icon/navigation/ChevronRight'
import { PropType } from 'vue'
import AddNote from '@featherds/icon/hardware/Network'
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
import { PointerAlignment, PopoverPlacement } from '@featherds/tooltip'
const discoveryStore = useDiscoveryStore()

const discoveryTypeList = [
  {
    title: 'Active Discovery',
    subtitle: 'Query nodes and cloud APIs to detect the entities to monitor.',
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
    subtitle: 'Use SNMP traps to identify network devices. You can configure only one passive discovery.',
    discoveryTypes: [
      {
        title: 'SNMP Traps',
        icon: AddNote,
        subtitle:
          'Identify devices through events, flows, and indirectly by evaluating other devices\' configuration settings.',
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
.subtitle .pop {
  text-decoration: underline;
  color: var(--feather-text-on-surface);
  cursor: pointer;
}
.subtitle .full {
  color: var(--feather-clickable-normal);
  margin-top: 12px;
  display: block;
}
.subtitle {
  :deep(.feather-popover-container) {
    z-index: 2;
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
  border-radius: vars.$border-radius-xs;
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
}
h5 {
  margin-bottom: 0;
  line-height: 1rem;
}
.t-subtitle {
  font-size: 12px;
  font-family: var(--feather-header-font-family);
  margin-top: 0;
}
</style>
