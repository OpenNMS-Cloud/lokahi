<template>
  <div
    class="syslog-snmp-traps-form"
    data-test="syslog-snmp-traps-form"
  >
    <div class="headline-action">
      <div
        class="headline"
        data-test="headline"
      >
        {{ DiscoverySyslogSNMPTrapsForm.headline }}
      </div>
    </div>
    <form
      @submit.prevent="submitHandler"
      data-test="form"
    >
      <div class="form-content">
        <FeatherInput
          label="Name"
          v-model="discoveryInfo.name"
        />
        <DiscoveryLocationsAutocomplete
          @location-selected="(val) => setDiscoveryValues('location', val)"
          ref="locationsAutocompleteRef"
          data-test="locations-autocomplete"
          :preLoadedlocation="props.discovery?.location"
          type="single"
        />
        <DiscoveryHelpConfiguring data-test="help-configuring" />
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
            @is-content-invalid="isCommunityStringInvalidListerner"
            @content-formatted="(val) => setDiscoveryValues('snmpCommunities', val)"
            :contentType="ContentEditableType.CommunityString"
            :regexDelim="COMMUNITY_STRING.regexDelim"
            :default-content="COMMUNITY_STRING.default"
            :label="discoveryText.ContentEditable.CommunityString.label"
            class="community-string-input"
            ref="contentEditableCommunityStringRef"
            data-test="cmmunity-string-content-editable"
            :content="props.discovery?.snmpCommunities?.join(', ')"
          />
          <DiscoveryContentEditable
            @is-content-invalid="isUDPPortInvalidListener"
            @content-formatted="(val) => setDiscoveryValues('snmpPorts', val)"
            :contentType="ContentEditableType.UDPPort"
            :regexDelim="UDP_PORT.regexDelim"
            :default-content="UDP_PORT.default"
            :label="discoveryText.ContentEditable.UDPPort.label"
            class="udp-port-input"
            ref="contentEditableUDPPortRef"
            :content="props.discovery?.snmpPorts?.join(', ')"
            :tooltipText="Common.tooltip.PortHelpTooltp"
          />
        </div>
      </div>
      <div class="form-footer">
        <FeatherButton
          @click="cancelHandler"
          secondary
          data-test="btn-cancel"
          >{{ discoveryText.Discovery.button.cancel }}</FeatherButton
        >
        <ButtonWithSpinner
          :isFetching="discoveryMutations.isFetchingPassiveDiscovery"
          :disabled="isFormInvalid"
          primary
          type="submit"
          data-test="btn-submit"
        >
          {{ discoveryText.Discovery.button.submit }}
        </ButtonWithSpinner>
      </div>
    </form>
  </div>
</template>

<script lang="ts" setup>
import discoveryText from './discovery.text'
import { DiscoverySyslogSNMPTrapsForm, Common } from './discovery.text'
import { ContentEditableType, COMMUNITY_STRING, UDP_PORT } from './discovery.constants'
import { useTagQueries } from '@/store/Queries/tagQueries'
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { useDiscoveryMutations } from '@/store/Mutations/discoveryMutations'
import { PassiveDiscovery, PassiveDiscoveryUpsertInput } from '@/types/graphql'
import { set } from 'lodash'

const props = defineProps<{
  discovery?: PassiveDiscovery | null
  successCallback: (name: string) => void
  cancel: () => void
}>()

const tagQueries = useTagQueries()
const discoveryQueries = useDiscoveryQueries()
const discoveryMutations = useDiscoveryMutations()
const discoveryInfo = ref<PassiveDiscoveryUpsertInput>(props.discovery || ({} as PassiveDiscoveryUpsertInput))
const isFormInvalid = ref(
  computed(() => {
    return isCommunityStringInvalid || isUDPPortInvalid
  })
)

watch(props, () => {
  discoveryInfo.value = props.discovery || ({} as PassiveDiscoveryUpsertInput)
})

const tagsAutocompleteRef = ref()
const tagsSelectedListener = (tags: Record<string, string>[]) => {
  discoveryInfo.value.tags = tags.map((tag) => {
    delete tag._text
    return tag
  })
}

const contentEditableCommunityStringRef = ref()
const contentEditableUDPPortRef = ref()

let isCommunityStringInvalid = false
const isCommunityStringInvalidListerner = (isInvalid: boolean) => (isCommunityStringInvalid = isInvalid)

let communityStringEntered: string[] = []
const communityStringEnteredListerner = (val: string[]) => (communityStringEntered = val)

let isUDPPortInvalid = false
const isUDPPortInvalidListener = (isInvalid: boolean) => (isUDPPortInvalid = isInvalid)

const setDiscoveryValues = (property: string, val: (string | number)[] | null) => {
  set(discoveryInfo.value, property, val)
}

const submitHandler = async () => {
  contentEditableCommunityStringRef.value.validateAndFormat()
  contentEditableUDPPortRef.value.validateAndFormat()
  await discoveryMutations.upsertPassiveDiscovery({ passiveDiscovery: discoveryInfo.value })

  if (!discoveryMutations.passiveDiscoveryError && discoveryInfo.value.name) {
    props.successCallback(discoveryInfo.value.name)
    resetForm()
    discoveryQueries.getDiscoveries()
    discoveryInfo.value = {} as PassiveDiscoveryUpsertInput
  }
}

const resetForm = () => {
  tagsAutocompleteRef.value.reset()
  contentEditableCommunityStringRef.value.reset()
  contentEditableUDPPortRef.value.reset()
  discoveryInfo.value = {} as PassiveDiscoveryUpsertInput
}

const cancelHandler = () => {
  resetForm()
  props.cancel()
}
</script>

<style lang="scss" scoped>
@use '@featherds/styles/mixins/typography';
@use '@featherds/styles/themes/variables';
@use '@/styles/mediaQueriesMixins.scss';

.headline-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(variables.$spacing-m);
}

.headline {
  @include typography.headline4;
}

.form-content {
  > * {
    margin-bottom: var(variables.$spacing-l);
    &:last-child {
      margin-bottom: 0;
    }
  }
}

.tags-autocomplete {
  width: 100%;

  @include mediaQueriesMixins.screen-xl {
    width: 49%;
  }
}

.content-editable-container {
  > div[class$='-input'] {
    margin-bottom: var(variables.$spacing-m);
  }

  @include mediaQueriesMixins.screen-xl {
    width: 100%;
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: flex-end;
    > div {
      width: 49%;
    }
  }
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: var(variables.$spacing-s);
}

:deep(.feather-input-sub-text) {
  display: none;
}
</style>
