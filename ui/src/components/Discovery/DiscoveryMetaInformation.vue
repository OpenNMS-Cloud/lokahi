<template>
  <div>
    <div :class="['flex-boxes', isICMP ? 'threefer' : 'twofer']">
      <FeatherTextarea
        v-if="isICMP"
        :modelValue="(discovery?.meta as DiscoverySNMPMeta)?.ipRanges?.join(';')"
        @updateModelValue="(e: string) => updateDiscoveryValue('ipRanges',e)"
        label="ip addresses"
      />
      <FeatherTextarea
        v-if="isSnmpTrapOrICMPV1"
        :modelValue="(discovery?.meta as DiscoveryTrapMeta)?.communityStrings?.join(',')"
        @updateModelValue="(e: string) => updateDiscoveryValue('communityStrings',e)"
        label="community string"
      />
      <FeatherTextarea
        v-if="isSnmpTrapOrICMPV1"
        :modelValue="(discovery?.meta as DiscoveryTrapMeta)?.udpPorts?.join(',')"
        @updateModelValue="(e: string) => updateDiscoveryValue('udpPorts',e)"
        label="udp port"
      />
    </div>
    <div v-if="isICMPV3">
      <h5>SNMP V3 Security Setting</h5>
      <div class="flex">
        <FeatherInput
          label="Username"
          :modelValue="(discovery?.meta as DiscoverySNMPV3Auth).username"
          @updateModelValue="(e: string) => updateDiscoveryValue('username',e)"
        />
        <FeatherInput
          label="Context"
          :modelValue="(discovery?.meta as DiscoverySNMPV3Auth).context"
          @updateModelValue="(e: string) => updateDiscoveryValue('context',e)"
        />
      </div>
    </div>
    <div v-if="isICMPV3WithPass">
      <div class="flex">
        <AtomicAutocomplete inputLabel="Auth" />
        <FeatherInput
          class="password"
          type="password"
          label="Password"
          :modelValue="(discovery?.meta as DiscoverySNMPV3Auth).password"
          @updateModelValue="(e: string) => updateDiscoveryValue('password',e)"
        />
        <FeatherCheckbox
          :modelValue="(discovery?.meta as DiscoverySNMPV3Auth).usePasswordAsKey"
          @updateModelValue="(e: string) => updateDiscoveryValue('usePasswordAsKey',e)"
          >Use password as key</FeatherCheckbox
        >
      </div>
    </div>
    <div v-if="[DiscoveryType.ICMPV3AuthPrivacy].includes(discovery.type as DiscoveryType)">
      <div class="flex">
        <AtomicAutocomplete inputLabel="Privacy" />
        <FeatherInput
          class="password"
          type="password"
          label="Password"
          :modelValue="(discovery?.meta as DiscoverySNMPV3AuthPrivacy).privacy"
          @updateModelValue="(e: string) => updateDiscoveryValue('privacy',e)"
        />
        <FeatherCheckbox
          :modelValue="(discovery?.meta as DiscoverySNMPV3AuthPrivacy).usePrivacyAsKey"
          @updateModelValue="(e: string) => updateDiscoveryValue('usePrivacyAsKey',e)"
          >Use password as key</FeatherCheckbox
        >
      </div>
    </div>
    <div v-if="[DiscoveryType.Azure].includes(discovery.type as DiscoveryType)">
      <div class="azure-row">
        <FeatherInput
          label="Client ID"
          :modelValue="(discovery?.meta as DiscoveryAzureMeta).clientId"
          @updateModelValue="(e: string) => updateDiscoveryValue('clientId',e)"
        />
        <FeatherInput
          label="Client Secret"
          :modelValue="(discovery?.meta as DiscoveryAzureMeta).clientSecret"
          @updateModelValue="(e: string) => updateDiscoveryValue('clientSecret',e)"
        />
      </div>
      <div class="azure-row">
        <FeatherInput
          label="Client Subscription ID"
          :modelValue="(discovery?.meta as DiscoveryAzureMeta).clientSubscriptionId"
          @updateModelValue="(e: string) => updateDiscoveryValue('clientSubscriptionId',e)"
        />
        <FeatherInput
          label="Directory ID"
          :modelValue="(discovery?.meta as DiscoveryAzureMeta).directoryId"
          @updateModelValue="(e: string) => updateDiscoveryValue('directoryId',e)"
        />
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { PropType } from 'vue'
import { DiscoveryType } from './discovery.constants'
import {
  NewOrUpdatedDiscovery,
  DiscoverySNMPMeta,
  DiscoveryTrapMeta,
  DiscoverySNMPV3Auth,
  DiscoverySNMPV3AuthPrivacy,
  DiscoveryAzureMeta
} from '@/types/discovery'

const props = defineProps({
  discovery: { type: Object as PropType<NewOrUpdatedDiscovery>, default: () => ({}) },
  updateDiscoveryValue: { type: Function as PropType<(key: string, value: string) => void>, default: () => ({}) }
})

const isSnmpTrapOrICMPV1 = computed(() =>
  [DiscoveryType.ICMP, DiscoveryType.SyslogSNMPTraps].includes(props.discovery.type as DiscoveryType)
)
const isICMP = computed(() =>
  [DiscoveryType.ICMP, DiscoveryType.ICMPV3Auth, DiscoveryType.ICMPV3AuthPrivacy, DiscoveryType.ICMPV3NoAuth].includes(
    props.discovery.type as DiscoveryType
  )
)
const isICMPV3 = computed(() =>
  [DiscoveryType.ICMPV3Auth, DiscoveryType.ICMPV3AuthPrivacy, DiscoveryType.ICMPV3NoAuth].includes(
    props.discovery.type as DiscoveryType
  )
)
const isICMPV3WithPass = computed(() =>
  [DiscoveryType.ICMPV3Auth, DiscoveryType.ICMPV3AuthPrivacy].includes(props.discovery.type as DiscoveryType)
)
</script>
<style lang="scss" scoped>
.azure-row {
  display: flex;
  width: 100%;
  gap: 2%;
  > :deep(div) {
    flex-basis: 50%;
  }
}
.inline {
  display: inline-block;
  max-width: calc(33% - 8px);
  margin-right: 12px;
  width: 100%;
}
.zip {
  display: inline;
}
.flex {
  display: flex;
  gap: 2%;
  :deep(.feather-input-container) {
    flex-basis: 50%;
  }
}
.password {
  min-width: 300px;
  margin-left: 12px;
  margin-right: 12px;
}
.flex-boxes {
  width: 100%;
  display: flex;
  gap: 2%;
}
.flex-boxes.threefer {
  :deep(.feather-textarea-container) {
    flex-basis: 33.333%;
  }
}
.flex-boxes.twofer {
  :deep(.feather-textarea-container) {
    flex-basis: 50%;
  }
}
</style>
