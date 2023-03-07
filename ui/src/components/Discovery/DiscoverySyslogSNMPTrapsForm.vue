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
          v-model="name"
        />
        <DiscoveryLocationsAutocomplete
          @location-selected="locationsSelectedListener"
          ref="locationsAutocompleteRef"
          data-test="locations-autocomplete"
          type="single"
        />
        <DiscoveryHelpConfiguring data-test="help-configuring" />
        <BasicAutocomplete
          @items-selected="tagsSelectedListener"
          :get-items="discoveryQueries.getTagsSearch"
          :items="discoveryQueries.tagsSearched"
          :label="Common.tagsInput"
          ref="tagsAutocompleteRef"
          data-test="tags-autocomplete"
        />
        <div class="content-editable-container">
          <DiscoveryContentEditable
            @is-content-invalid="isCommunityStringInvalidListerner"
            @content-formatted="communityStringEnteredListerner"
            :contentType="ContentEditableType.CommunityString"
            :regexDelim="COMMUNITY_STRING.regexDelim"
            :default-content="COMMUNITY_STRING.default"
            :label="discoveryText.ContentEditable.CommunityString.label"
            class="community-string-input"
            ref="contentEditableCommunityStringRef"
            data-test="cmmunity-string-content-editable"
          />
          <DiscoveryContentEditable
            @is-content-invalid="isUDPPortInvalidListener"
            @content-formatted="UDPPortEnteredListener"
            :contentType="ContentEditableType.UDPPort"
            :regexDelim="UDP_PORT.regexDelim"
            :default-content="UDP_PORT.default"
            :label="discoveryText.ContentEditable.UDPPort.label"
            class="udp-port-input"
            ref="contentEditableUDPPortRef"
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
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { useDiscoveryMutations } from '@/store/Mutations/discoveryMutations'
import { Location } from '@/types/graphql'

const props = defineProps<{
  successCallback: (name: string) => void
  cancel: () => void
}>()

const discoveryQueries = useDiscoveryQueries()
const discoveryMutations = useDiscoveryMutations()
const name = ref()

const isFormInvalid = ref(
  computed(() => {
    return isCommunityStringInvalid || isUDPPortInvalid
  })
)

const locationsAutocompleteRef = ref()
let locationsSelected = ''
const locationsSelectedListener = (locations: Location[]) => {
  if (locations[0].location) locationsSelected = locations[0].location
}

const tagsAutocompleteRef = ref()
let tagsSelected: Record<string, string>[] = []
const tagsSelectedListener = (tags: Record<string, string>[]) => {
  tagsSelected = tags.map((tag) => {
    delete tag._text
    return tag
  })
}

const contentEditableCommunityStringRef = ref()
const contentEditableUDPPortRef = ref()

let isCommunityStringInvalid = false
const isCommunityStringInvalidListerner = (isInvalid: boolean) => isCommunityStringInvalid = isInvalid

let communityStringEntered: string[] = []
const communityStringEnteredListerner = (val: string[]) => communityStringEntered = val

let isUDPPortInvalid = false
const isUDPPortInvalidListener = (isInvalid: boolean) => isUDPPortInvalid = isInvalid

let UDPPortEntered: number[] = []
const UDPPortEnteredListener = (val: number[]) => UDPPortEntered = val

const submitHandler = async () => {
  contentEditableCommunityStringRef.value.validateAndFormat()
  contentEditableUDPPortRef.value.validateAndFormat()
  
  const passiveDiscovery = {
    location: locationsSelected,
    name: name.value,
    snmpCommunities: communityStringEntered,
    snmpPorts: UDPPortEntered,
    tags: tagsSelected
  }

  await discoveryMutations.upsertPassiveDiscovery({ passiveDiscovery })

  if (!discoveryMutations.passiveDiscoveryError) {
    props.successCallback(name.value)
    resetForm()
    discoveryQueries.getDiscoveries()
  }
}

const resetForm = () => {
  tagsAutocompleteRef.value.reset()
  contentEditableCommunityStringRef.value.reset()
  contentEditableUDPPortRef.value.reset()
  name.value = ''
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

.discovery-autocomplete {
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
  border-top: 1px solid var(variables.$border-on-surface);
  padding-top: var(variables.$spacing-m);
}
</style>
