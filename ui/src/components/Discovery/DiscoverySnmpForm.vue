<template>
  <form
    @submit.prevent="saveHandler"
    novalidate
    v-tabindex
    class="form"
  >
    <div class="form-title">{{ DiscoverySNMPForm.title }}</div>
    <FeatherInput
      v-model="discoveryInfo.name"
      :label="DiscoverySNMPForm.nameInputLabel"
      class="name-input"
      :schema="nameV"
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
      :preselectedItems="tags"
    />
    <div class="content-editable-container">
      <DiscoveryContentEditable
        @content-formatted="(val) => setSnmpConfig('ipAddresses', val)"
        :contentType="ContentEditableType.IP"
        :regexDelim="IP_RANGE.regexDelim"
        :regexExpression="REGEX_EXPRESSIONS.IP"
        :label="discoveryText.ContentEditable.IP.label"
        ref="contentEditableIPRef"
        class="ip-input"
        :tooltipText="Common.tooltipIP"
        :content="props.discovery?.ipAddresses?.join(', ')"
        isRequired
        :id="1"
      />
      <DiscoveryContentEditable
        @content-formatted="(val) => setSnmpConfig('snmpConfig.readCommunities', val)"
        :contentType="ContentEditableType.CommunityString"
        :regexDelim="COMMUNITY_STRING.regexDelim"
        :label="discoveryText.ContentEditable.CommunityString.label"
        :default-content="COMMUNITY_STRING.default"
        ref="contentEditableCommunityStringRef"
        class="community-input"
        :content="props.discovery?.snmpConfig?.readCommunities?.join(', ')"
        :id="2"
      />
      <DiscoveryContentEditable
        @content-formatted="(val) => setSnmpConfig('snmpConfig.ports', val)"
        :contentType="ContentEditableType.UDPPort"
        :regexDelim="UDP_PORT.regexDelim"
        :regexExpression="REGEX_EXPRESSIONS.PORT"
        :label="discoveryText.ContentEditable.UDPPort.label"
        :default-content="UDP_PORT.default"
        class="udp-port-input"
        ref="contentEditableUDPPortRef"
        :tooltipText="Common.tooltipPort"
        :content="props.discovery?.snmpConfig?.ports?.join(', ')"
        :id="3"
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
import {
  ContentEditableType,
  UDP_PORT,
  COMMUNITY_STRING,
  IP_RANGE,
  REGEX_EXPRESSIONS
} from '@/components/Discovery/discovery.constants'
import discoveryText, { DiscoverySNMPForm, Common } from '@/components/Discovery/discovery.text'
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { useTagQueries } from '@/store/Queries/tagQueries'
import { IcmpActiveDiscovery, IcmpActiveDiscoveryCreateInput } from '@/types/graphql'
import { set } from 'lodash'
import { useDiscoveryMutations } from '@/store/Mutations/discoveryMutations'
import DiscoveryContentEditable from '@/components/Discovery/DiscoveryContentEditable.vue'
import { useForm } from '@featherds/input-helper'
import { string } from 'yup'

const form = useForm()
const { createDiscoveryConfig, activeDiscoveryError, isFetchingActiveDiscovery } = useDiscoveryMutations()
const tagQueries = useTagQueries()
const discoveryQueries = useDiscoveryQueries()

const props = defineProps<{
  discovery?: IcmpActiveDiscovery | null
  successCallback: (name: string) => void
  cancel: () => void
}>()

const nameV = string().required('Name is required.')
const discoveryInfo = ref<IcmpActiveDiscovery | IcmpActiveDiscoveryCreateInput>(
  props.discovery || ({} as IcmpActiveDiscoveryCreateInput)
)
const contentEditableIPRef = ref<InstanceType<typeof DiscoveryContentEditable>>()
const contentEditableCommunityStringRef = ref<InstanceType<typeof DiscoveryContentEditable>>()
const contentEditableUDPPortRef = ref<InstanceType<typeof DiscoveryContentEditable>>()
const tags = computed(() => (props.discovery?.id ? discoveryQueries.tagsByActiveDiscoveryId : []))
const tagsAutocompleteRef = ref()

onMounted(() => {
  if (props.discovery?.id) {
    discoveryQueries.getTagsByActiveDiscoveryId(props.discovery.id)
  } else {
    // add default tag to snmp form
    tagsAutocompleteRef.value.addValue('default')
  }
})

watch(props, () => {
  if (props.discovery?.id) {
    form.clearErrors()
    discoveryQueries.getTagsByActiveDiscoveryId(props.discovery.id)
  }
  discoveryInfo.value = props.discovery || ({} as IcmpActiveDiscoveryCreateInput)
})

const setSnmpConfig = (property: string, val: (string | number)[] | null) => {
  set(discoveryInfo.value, property, val)
}

const tagsSelectedListener = (tags: Record<string, string>[]) => {
  ;(discoveryInfo.value as IcmpActiveDiscoveryCreateInput).tags = tags.map(({ name }) => ({ name }))
}

const resetContentEditable = () => {
  tagsAutocompleteRef.value.reset()
  contentEditableIPRef.value?.reset()
  contentEditableCommunityStringRef.value?.reset()
  contentEditableUDPPortRef.value?.reset()
}

const saveHandler = async () => {
  contentEditableCommunityStringRef.value?.validateContent()
  const isIpInvalid = contentEditableIPRef.value?.validateContent()
  const isPortInvalid = contentEditableUDPPortRef.value?.validateContent()
  if (form.validate().length || isIpInvalid || isPortInvalid) return
  await createDiscoveryConfig({ request: discoveryInfo.value })
  if (!activeDiscoveryError.value && discoveryInfo.value.name) {
    discoveryQueries.getDiscoveries()
    resetContentEditable()
    props.successCallback(discoveryInfo.value.name)
    discoveryInfo.value = {} as IcmpActiveDiscoveryCreateInput
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
    margin-top: var(variables.$spacing-xs);
    margin-bottom: var(variables.$spacing-xl);
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
  margin-top: var(variables.$spacing-m);
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
    align-items: flex-start;
    > div {
      width: 32%;
    }
  }
}
</style>
