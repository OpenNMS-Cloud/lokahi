<template>
  <div
    v-tabindex
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
          :schema="nameV"
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
          :preselectedItems="tags"
        />
        <div class="content-editable-container">
          <DiscoveryContentEditable
            @content-formatted="(val) => setDiscoveryValues('snmpCommunities', val)"
            :contentType="ContentEditableType.CommunityString"
            :regexDelim="COMMUNITY_STRING.regexDelim"
            :default-content="COMMUNITY_STRING.default"
            :label="discoveryText.ContentEditable.CommunityString.label"
            class="community-string-input"
            ref="contentEditableCommunityStringRef"
            data-test="cmmunity-string-content-editable"
            :content="props.discovery?.snmpCommunities?.join(', ')"
            :id="1"
          />
          <DiscoveryContentEditable
            @content-formatted="(val) => setDiscoveryValues('snmpPorts', val)"
            :contentType="ContentEditableType.UDPPort"
            :regexDelim="UDP_PORT.regexDelim"
            :regexExpression="REGEX_EXPRESSIONS.PORT"
            :default-content="UDP_PORT.default"
            :label="discoveryText.ContentEditable.UDPPort.label"
            class="udp-port-input"
            ref="contentEditableUDPPortRef"
            :content="props.discovery?.snmpPorts?.join(', ')"
            :tooltipText="Common.tooltipPort"
            :id="2"
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
import { ContentEditableType, COMMUNITY_STRING, UDP_PORT, REGEX_EXPRESSIONS } from './discovery.constants'
import { useTagQueries } from '@/store/Queries/tagQueries'
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { useDiscoveryMutations } from '@/store/Mutations/discoveryMutations'
import { PassiveDiscovery, PassiveDiscoveryUpsertInput } from '@/types/graphql'
import { cloneDeep, set } from 'lodash'
import { useForm } from '@featherds/input-helper'
import { string } from 'yup'
const form = useForm()
const nameV = string().required('Name is required.')

const props = defineProps<{
  discovery?: PassiveDiscovery | null
  successCallback: (name: string) => void
  cancel: () => void
}>()

const tagQueries = useTagQueries()
const discoveryQueries = useDiscoveryQueries()
const discoveryMutations = useDiscoveryMutations()
const discoveryInfo = ref<PassiveDiscoveryUpsertInput>(props.discovery || ({} as PassiveDiscoveryUpsertInput))
const tags = computed(() => (props.discovery?.id ? discoveryQueries.tagsByPassiveDiscoveryId : []))

onMounted(() => {
  if (props.discovery?.id) {
    discoveryQueries.getTagsByPassiveDiscoveryId(props.discovery.id)
  } else {
    // add default tag to syslog form
    tagsAutocompleteRef.value.addValue('default')
  }
})

watchEffect(async () => {
  if (props.discovery?.id) {
    form.clearErrors()
    await discoveryQueries.getTagsByPassiveDiscoveryId(props.discovery.id)
    discoveryInfo.value.tags = tags.value?.map((tag) => ({ name: tag.name }))
  }
  discoveryInfo.value = props.discovery || ({} as PassiveDiscoveryUpsertInput)
})

const tagsAutocompleteRef = ref()
const tagsSelectedListener = (tags: Record<string, string>[]) => {
  discoveryInfo.value.tags = tags.map((tag) => ({ name: tag.name }))
}

const contentEditableCommunityStringRef = ref()
const contentEditableUDPPortRef = ref()

const setDiscoveryValues = (property: string, val: (string | number)[] | null) => {
  set(discoveryInfo.value, property, val)
}

const submitHandler = async () => {
  contentEditableCommunityStringRef.value.validateContent()
  const isPortInvalid = contentEditableUDPPortRef.value?.validateContent()
  if (form.validate().length || isPortInvalid) return

  // clone and remove unused props from payload
  const payload = cloneDeep(discoveryInfo.value) as Partial<PassiveDiscovery>
  if (payload.toggle) delete payload.toggle

  await discoveryMutations.upsertPassiveDiscovery({ passiveDiscovery: payload })

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
    margin-bottom: var(variables.$spacing-s);
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
    align-items: flex-start;
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
</style>
