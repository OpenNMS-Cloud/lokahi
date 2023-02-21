<template>
  <div class="syslog-snmp-traps-form">
    <div class="headline-action">
      <div class="headline">{{ DiscoverySyslogSNMPTrapsForm.headline }}</div>
      <!-- <Icon
        @click="deleteHandler"
        :icon="deleteIcon"
      /> -->
    </div>
    <form
      @submit.prevent="submitHandler"
      novalidate
    >
      <div class="form-content">
        <!-- location -->
        <LocationsAutocomplete @location-selected="locationsSelectedListener" />
        <DiscoveryHelpConfiguring />
        <DiscoveryAutocomplete
          @items-selected="tagsSelectedListener"
          :get-items="discoveryQueries.getTagsUponTyping"
          :items="discoveryQueries.tagsUponTyping"
          :label="Common.tagsInput"
          :schema="inputV.tags"
          required
        />
        <FeatherAutocomplete
          class="my-autocomplete"
          label="Users"
          v-model="valueBar"
          :loading="loading"
          :results="results"
          type="single"
          @search="search"
          :schema="fooV"
          required
        ></FeatherAutocomplete>
        <div class="content-editable-container">
          <DiscoveryContentEditable
            @is-content-invalid="isCommunityStringInvalidListerner"
            @content-formatted="communityStringEnteredListerner"
            ref="contentEditableCommunityStringRef"
            :contentType="communityString.type"
            :regexDelim="communityString.regexDelim"
            :label="communityString.label"
            :default-content="'someCommunityString'"
            class="community-string-input"
          />
          <DiscoveryContentEditable
            @is-content-invalid="isUDPPortInvalidListener"
            @content-formatted="UDPPortEnteredListener"
            ref="contentEditableUDPPortRef"
            :contentType="udpPort.type"
            :regexDelim="udpPort.regexDelim"
            :label="udpPort.label"
            :default-content="'someUDPPort'"
            class="udp-port-input"
          />
        </div>
      </div>
      <div class="form-footer">
        <FeatherButton
          @click="cancel"
          secondary
          >{{ discoveryText.Discovery.button.cancel }}</FeatherButton
        >
        <ButtonWithSpinner
          :isFetching="discoveryMutations.savingSyslogSNMPTraps"
          primary
          type="submit"
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
import { string } from 'yup'
import { useForm } from '@featherds/input-helper'

const names = ['William', 'Chris', 'Jane', 'Smith']
let timeout = -1
const loading = ref(false)
const results = ref([])
const valueBar = ref(undefined)
const fooV = string().required('Required')
const search = (q: string) => {
  console.log('q', q)
  const form = useForm()
  const errors = form.validate()
  console.log('errors', errors)
  loading.value = true
  clearTimeout(timeout)
  timeout = window.setTimeout(() => {
    results.value = names
      .filter((x) => x.toLowerCase().indexOf(q) > -1)
      .map((x) => ({
        _text: x
      }))
    loading.value = false
    console.log('results.value', results.value)
    console.log('valueBar.value', valueBar.value)
  }, 500)
}

const props = defineProps<{
  successCallback: (name: string) => void
  cancel: () => void
}>()

const { showSnackbar } = useSnackbar()

const discoveryQueries = useDiscoveryQueries()
const discoveryMutations = useDiscoveryMutations()

/* const isFormInvalid = ref(
  computed(() => {
    return isCommunityStringInvalid || isUDPPortInvalid
  })
) */

let tagsSelected: Record<string, string>[] = []
const tagsSelectedListener = (tags: Record<string, string>[]) => {
  tagsSelected = tags
}

let locationsSelected: Record<string, string>[] = []
const locationsSelectedListener = (locations: Record<string, string>[]) => {
  locationsSelected = locations
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

const inputV = {
  tags: string().required('Required')
}
const form = useForm()

const submitHandler = async () => {
  const validationErrors = form.validate()
  console.log('validationErrors', validationErrors)
  if (!validationErrors.length) {
    contentEditableCommunityStringRef.value.validateAndFormat()
    contentEditableUDPPortRef.value.validateAndFormat()

    const results: any = await discoveryMutations.saveSyslogSNMPTraps({
      locations: locationsSelected,
      tagsSelected: tagsSelected,
      communityString: communityStringEntered,
      UDPPort: UDPPortEntered
    })

    if (results.data) {
      showSnackbar({
        msg: 'Save Successfully!'
      })

      props.successCallback(results.data.saveDiscovery[0].name)
    } else {
      showSnackbar({
        msg: results.errors[0].message,
        error: true
      })
    }
  }
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
