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
      <!-- <Icon
        @click="deleteHandler"
        :icon="deleteIcon"
      /> -->
    </div>
    <form
      @submit.prevent="submitHandler"
      data-test="form"
    >
      <div class="form-content">
        <LocationsAutocomplete
          @location-selected="locationsSelectedListener"
          ref="locationsAutocompleteRef"
          data-test="locations-autocomplete"
        />
        <DiscoveryHelpConfiguring data-test="help-configuring" />
        <DiscoveryAutocomplete
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
            :contentType="communityString.type"
            :regexDelim="communityString.regexDelim"
            :label="communityString.label"
            :default-content="''"
            class="community-string-input"
            ref="contentEditableCommunityStringRef"
            data-test="cmmunity-string-content-editable"
          />
          <DiscoveryContentEditable
            @is-content-invalid="isUDPPortInvalidListener"
            @content-formatted="UDPPortEnteredListener"
            :contentType="udpPort.type"
            :regexDelim="udpPort.regexDelim"
            :label="udpPort.label"
            :default-content="''"
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
          :isFetching="discoveryMutations.savingSyslogSNMPTraps"
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
import DeleteIcon from '@featherds/icon/action/Delete'
import { IIcon } from '@/types'
import discoveryText from './discovery.text'
import { DiscoverySyslogSNMPTrapsForm, Common } from './discovery.text'
import { ContentEditableType } from './discovery.constants'
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { useDiscoveryMutations } from '@/store/Mutations/discoveryMutations'
import useSnackbar from '@/composables/useSnackbar'

const props = defineProps<{
  successCallback: (name: string) => void
  cancel: () => void
}>()

const { showSnackbar } = useSnackbar()

const discoveryQueries = useDiscoveryQueries()
const discoveryMutations = useDiscoveryMutations()

const isFormInvalid = ref(
  computed(() => {
    return isCommunityStringInvalid || isUDPPortInvalid
  })
)

const locationsAutocompleteRef = ref()
let locationsSelected: Record<string, string>[] = []
const locationsSelectedListener = (locations: Record<string, string>[]) => {
  locationsSelected = locations
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
const communityString = {
  type: ContentEditableType.CommunityString,
  regexDelim: '',
  label: discoveryText.ContentEditable.CommunityString.label
}

let isCommunityStringInvalid = false
const isCommunityStringInvalidListerner = (isInvalid: boolean) => {
  isCommunityStringInvalid = isInvalid
}
let communityStringEntered = ''
const communityStringEnteredListerner = (str: string) => {
  communityStringEntered = str
}

const contentEditableUDPPortRef = ref()
const udpPort = {
  type: ContentEditableType.UDPPort,
  regexDelim: '',
  label: discoveryText.ContentEditable.UDPPort.label
}

let isUDPPortInvalid = false
const isUDPPortInvalidListener = (isInvalid: boolean) => {
  isUDPPortInvalid = isInvalid
}
let UDPPortEntered = ''
const UDPPortEnteredListener = (str: string) => {
  UDPPortEntered = str
}

const submitHandler = async () => {
  contentEditableCommunityStringRef.value.validateAndFormat()
  contentEditableUDPPortRef.value.validateAndFormat()

  const results: any = await discoveryMutations.saveSyslogSNMPTraps({
    locations: locationsSelected,
    tagsSelected: tagsSelected,
    communityString: communityStringEntered,
    UDPPort: UDPPortEntered
  })

  if (results.data) {
    props.successCallback(results.data.saveDiscovery[0].name)
    resetForm()
  } else {
    showSnackbar({
      msg: results.errors[0].message,
      error: true
    })
  }
}

const resetForm = () => {
  // locationsAutocompleteRef.value.reset()
  tagsAutocompleteRef.value.reset()
  contentEditableCommunityStringRef.value.reset()
  contentEditableUDPPortRef.value.reset()
}

const cancelHandler = () => {
  resetForm()
  props.cancel()
}

const deleteHandler = () => {
  console.log('delete')
}

const deleteIcon: IIcon = {
  image: markRaw(DeleteIcon),
  tooltip: 'Delete',
  size: '1.5rem',
  cursorHover: 'pointer'
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
