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
      :preLoadedlocation="props.discovery?.location"
      @location-selected="(val) => setSnmpConfig('location', val)"
    />
    <BasicAutocomplete
      @items-selected="tagsSelectedListener"
      :get-items="tagQueries.getTagsSearch"
      :items="tagQueries.tagsSearched"
      :label="Common.tagsInput"
      ref="tagsAutocompleteRef"
      class="tags-autocomplete"
      data-test="tags-autocomplete"
    />
    <div class="content-editable-container">
      <DiscoveryContentEditable
        @is-content-invalid="isIPRangeInvalidListener"
        @content-formatted="(val) => setSnmpConfig('ipAddresses', val)"
        :contentType="ContentEditableType.IP"
        :regexDelim="IP_RANGE.regexDelim"
        :label="discoveryText.ContentEditable.IP.label"
        ref="contentEditableIPRef"
        class="ip-input"
        :tooltipText="Common.tooltip.IPHelpTooltp"
        :content="props.discovery?.ipAddresses?.join(', ')"
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
        :content="props.discovery?.snmpConfig?.readCommunities?.join(', ')"
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
        :tooltipText="Common.tooltip.PortHelpTooltp"
        :content="props.discovery?.snmpConfig?.ports?.join(', ')"
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
import discoveryText, { DiscoverySNMPForm, Common } from '@/components/Discovery/discovery.text'
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { useTagQueries } from '@/store/Queries/tagQueries'
import { Location, CreateDiscoveryConfigRequestInput } from '@/types/graphql'
import { set } from 'lodash'
import { useDiscoveryMutations } from '@/store/Mutations/discoveryMutations'
import DiscoveryContentEditable from '@/components/Discovery/DiscoveryContentEditable.vue'

const { createDiscoveryConfig, activeDiscoveryError, isFetchingActiveDiscovery } = useDiscoveryMutations()

const tagQueries = useTagQueries()
const discoveryQueries = useDiscoveryQueries()
const props = defineProps<{
  discovery?: CreateDiscoveryConfigRequestInput | null
  successCallback: (name: string) => void
  cancel: () => void
}>()

const discoveryInfo = ref<CreateDiscoveryConfigRequestInput>(
  props.discovery || ({} as CreateDiscoveryConfigRequestInput)
)
const contentEditableIPRef = ref<InstanceType<typeof DiscoveryContentEditable>>()
const contentEditableCommunityStringRef = ref<InstanceType<typeof DiscoveryContentEditable>>()
const contentEditableUDPPortRef = ref<InstanceType<typeof DiscoveryContentEditable>>()

watch(props, () => {
  discoveryInfo.value = props.discovery || ({} as CreateDiscoveryConfigRequestInput)
})

const setSnmpConfig = (property: string, val: (string | number)[] | null) => {
  set(discoveryInfo.value, property, val)
}

const tagsAutocompleteRef = ref()
const tagsSelectedListener = (tags: Record<string, string>[]) => {
  discoveryInfo.value.tags = tags.map(({ name }) => ({ name }))
}

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
  tagsAutocompleteRef.value.reset()
  contentEditableIPRef.value?.reset()
  contentEditableCommunityStringRef.value?.reset()
  contentEditableUDPPortRef.value?.reset()
}

const saveHandler = async () => {
  contentEditableIPRef.value?.validateAndFormat()
  contentEditableCommunityStringRef.value?.validateAndFormat()
  contentEditableUDPPortRef.value?.validateAndFormat()
  await createDiscoveryConfig({ CreateDiscoveryConfigRequestInput: discoveryInfo.value })

  if (!activeDiscoveryError.value && discoveryInfo.value.configName) {
    discoveryQueries.getDiscoveries()
    resetContentEditable()
    props.successCallback(discoveryInfo.value.configName)
    discoveryInfo.value = {} as CreateDiscoveryConfigRequestInput
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
  .locations-select {
    margin-top: var(variables.$spacing-xl);
    margin-bottom: var(variables.$spacing-l);
    width: 100%;
  }

  @include mediaQueriesMixins.screen-lg {
    .name-input,
    .locations-select,
    .tags-autocomplete {
      width: 49%;
    }
  }
}

.footer {
  display: flex;
  justify-content: flex-end;
}

.tags-autocomplete {
  margin-bottom: var(variables.$spacing-l);
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
  display: none;
}
</style>
