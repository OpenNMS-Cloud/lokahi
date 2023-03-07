<template>
  <form
    @submit.prevent="saveHandler"
    novalidate
    class="form"
  >
    <div class="form-title">{{ DiscoverySNMPForm.title }}</div>
    <FeatherInput
      v-model="discoveryInfo.configName"
      :label="DiscoverySNMPForm.nameInputLabel"
      class="name-input"
    />
    <DiscoveryLocationsAutocomplete
      class="locations-select"
      type="single"
      :preLoadedlocations="[discoveryInfo?.location]"
      @location-selected="setLocation"
    />
    <!--<BasicAutocomplete
      @items-selected="tagsSelectedListener"
      :get-items="discoveryQueries.getTagsSearch"
      :items="discoveryQueries.tagsSearched"
      :label="Common.tagsInput"
      ref="tagsAutocompleteRef"
      data-test="tags-autocomplete"
    />-->
    <div class="content-editable-container">
      <DiscoveryContentEditable
        @is-content-invalid="isIPRangeInvalidListener"
        @content-formatted="(val) => setSnmpConfig('ipAddresses', val)"
        :contentType="ContentEditableType.IP"
        :regexDelim="IP_RANGE.regexDelim"
        :label="discoveryText.ContentEditable.IP.label"
        ref="contentEditableIPRef"
        class="ip-input"
        :tooltipText="DiscoverySNMPForm.IPHelpTooltp"
      />
      <DiscoveryContentEditable
        @is-content-invalid="isCommunityStringInvalidListerner"
        @content-formatted="(val) => setSnmpConfig('snmpConfig.readCommunities', val)"
        :contentType="ContentEditableType.CommunityString"
        :regexDelim="COMMUNITY_STRING.regexDelim"
        :label="discoveryText.ContentEditable.CommunityString.label"
        :default-content="COMMUNITY_STRING.default"
        ref="contentEditableCommunityStringRef"
        class="community-input"
      />
      <DiscoveryContentEditable
        @is-content-invalid="isUDPPortInvalidListener"
        @content-formatted="(val) => setSnmpConfig('snmpConfig.ports', val)"
        :contentType="ContentEditableType.UDPPort"
        :regexDelim="UDP_PORT.regexDelim"
        :label="discoveryText.ContentEditable.UDPPort.label"
        :default-content="UDP_PORT.default"
        class="udp-port-input"
        ref="contentEditableUDPPortRef"
        :tooltipText="DiscoverySNMPForm.PortHelpTooltp"
      />
    </div>

    <div class="footer">
      <FeatherButton
        @click="cancel"
        secondary
        >{{ discoveryText.Discovery.button.cancel }}</FeatherButton
      >
      <ButtonWithSpinner
        v-if="!props.discovery"
        type="submit"
        primary
        :isFetching="isFetchingActiveDiscovery"
      >
        {{ discoveryText.Discovery.button.submit }}
      </ButtonWithSpinner>
    </div>
  </form>
</template>

<script lang="ts" setup>
import { ContentEditableType, UDP_PORT, COMMUNITY_STRING, IP_RANGE } from '@/components/Discovery/discovery.constants'
import discoveryText, { DiscoverySNMPForm } from '@/components/Discovery/discovery.text'
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { Location, ActiveDiscovery } from '@/types/graphql'
import { set } from 'lodash'
import { useDiscoveryMutations } from '@/store/Mutations/discoveryMutations'
import DiscoveryContentEditable from '@/components/Discovery/DiscoveryContentEditable.vue'

const { createDiscoveryConfig, activeDiscoveryError, isFetchingActiveDiscovery } = useDiscoveryMutations()

const discoveryQueries = useDiscoveryQueries()
const props = defineProps<{
  discovery?: ActiveDiscovery | null
  successCallback: (name: string) => void
  cancel: () => void
}>()

const discoveryInfo = ref<ActiveDiscovery>(props.discovery || ({} as ActiveDiscovery))
//const selectedTags = ref<string[]>()
const contentEditableIPRef = ref<InstanceType<typeof DiscoveryContentEditable>>()
const contentEditableCommunityStringRef = ref<InstanceType<typeof DiscoveryContentEditable>>()
const contentEditableUDPPortRef = ref<InstanceType<typeof DiscoveryContentEditable>>()

watch(props, () => {
  discoveryInfo.value = props.discovery || ({} as ActiveDiscovery)
})

const setLocation = (location: Location[]) => {
  if (location[0] && location[0].location) {
    discoveryInfo.value.location = location[0]?.location
  } else {
    discoveryInfo.value.location = undefined
  }
}

const setSnmpConfig = (property: string, val: (string | number)[] | null) => {
  set(discoveryInfo.value, property, val)
}

//const tagsAutocompleteRef = ref()
// const tagsSelectedListener = (tags: Record<string, string>[]) => {
//   selectedTags.value = tags.map((tag) => tag.name)
// }

const isIPRangeInvalidListener = (isInvalid: boolean) => {
  console.log(isInvalid)
}

const isCommunityStringInvalidListerner = (isInvalid: boolean) => {
  console.log(isInvalid)
}

const isUDPPortInvalidListener = (isInvalid: boolean) => {
  console.log(isInvalid)
}

const resetContentEditable = () => {
  // tagsAutocompleteRef.value.reset()
  contentEditableIPRef.value?.reset()
  contentEditableCommunityStringRef.value?.reset()
  contentEditableUDPPortRef.value?.reset()
}

const saveHandler = async () => {
  contentEditableIPRef.value?.validateAndFormat()
  contentEditableCommunityStringRef.value?.validateAndFormat()
  contentEditableUDPPortRef.value?.validateAndFormat()
  await createDiscoveryConfig({ activeDiscovery: discoveryInfo.value })
  if (!activeDiscoveryError && discoveryInfo.value.configName) {
    discoveryQueries.getDiscoveries()
    resetContentEditable()
    props.successCallback(discoveryInfo.value.configName)
    discoveryInfo.value = {} as ActiveDiscovery
  }
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@/styles/mediaQueriesMixins.scss';
@use '@/styles/vars.scss';
@use '@featherds/styles/mixins/typography';
.form {
  .form-title {
    @include typography.headline4;
    margin-bottom: var(variables.$spacing-m);
  }
  .name-input {
    width: 100%;
  }
  .locations-select {
    margin-top: var(variables.$spacing-xl);
    margin-bottom: var(variables.$spacing-s);
    width: 100%;
  }

  @include mediaQueriesMixins.screen-lg {
    .name-input,
    .locations-select {
      width: 49%;
    }
  }
}

.footer {
  display: flex;
  justify-content: flex-end;
}

.discovery-autocomplete {
  :deep(.chip-list) {
    margin-top: var(variables.$spacing-s);
    margin-bottom: var(variables.$spacing-s);
  }
}

.content-editable-container {
  display: flex;
  flex-direction: column;
  > div {
    margin-bottom: var(variables.$spacing-l);
  }
  @include mediaQueriesMixins.screen-xl {
    width: 100%;
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: flex-end;
    > div {
      width: 32%;
    }
  }
}

:deep(.feather-input-sub-text) {
  display: none !important;
}
</style>
