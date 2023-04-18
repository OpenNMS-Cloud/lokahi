<template>
  <div
    v-tabindex
    class="azure-container"
  >
    <div class="title">
      {{ Azure.title }}
    </div>

    <FeatherInput
      label="Azure Name"
      v-model="store.azure.name"
      class="name"
      :schema="nameV"
      data-test="azure-name-input"
    />

    <div class="row">
      <FeatherInput
        v-model="store.azure.clientId"
        label="ClientID"
        class="column"
        :schema="clientIdV"
        data-test="azure-client-input"
      />
      <FeatherProtectedInput
        v-model="store.azure.clientSecret"
        label="Client Secret"
        class="column"
        :schema="clientSecretV"
        data-test="azure-secret-input"
      />
    </div>

    <div class="row">
      <FeatherInput
        v-model="store.azure.subscriptionId"
        label="SubscriptionID"
        class="column"
        :schema="subIdV"
        data-test="azure-sub-input"
      />
      <FeatherInput
        v-model="store.azure.directoryId"
        label="DirectoryID"
        class="column"
        :schema="dirIdV"
        data-test="azure-dir-input"
      />
    </div>

    <DiscoveryLocationsAutocomplete
      @locationSelected="selectLocation"
      class="locations"
      type="single"
    />
    <BasicAutocomplete
      class="tags"
      @items-selected="tagsSelectedListener"
      :get-items="tagQueries.getTagsSearch"
      :items="tagQueries.tagsSearched"
      :label="Common.tagsInput"
      ref="tagsAutocompleteRef"
      :preselectedItems="tags"
    />

    <div class="buttons">
      <FeatherButton
        @click="cancel"
        secondary
      >
        {{ Azure.cancelBtnText }}
      </FeatherButton>
      <ButtonWithSpinner
        v-if="!discovery"
        type="submit"
        :isFetching="discoveryMutations.isFetching.value"
        @click="saveAzureDiscovery"
        primary
      >
        {{ Azure.saveBtnText }}
      </ButtonWithSpinner>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
import { Azure, Common } from './discovery.text'
import { AzureActiveDiscovery } from '@/types/graphql'
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { useTagQueries } from '@/store/Queries/tagQueries'
import { useDiscoveryMutations } from '@/store/Mutations/discoveryMutations'
import { useForm } from '@featherds/input-helper'
import { string } from 'yup'

const store = useDiscoveryStore()
const discoveryQueries = useDiscoveryQueries()
const tagQueries = useTagQueries()
const discoveryMutations = useDiscoveryMutations()
const form = useForm()

const props = defineProps<{
  successCallback: (name: string) => void
  cancel: () => void
  discovery: AzureActiveDiscovery | null
}>()

const tags = computed(() => (props.discovery?.id ? discoveryQueries.tagsByActiveDiscoveryId : []))

onMounted(() => {
  if (props.discovery?.id) {
    discoveryQueries.getTagsByActiveDiscoveryId(props.discovery.id)
  } else {
    // add default tag to azure form
    tagsAutocompleteRef.value.addValue('default')
  }
})

const selectLocation = (location: string) => location && store.selectLocation(location, true)

const tagsAutocompleteRef = ref()
const tagsSelectedListener = (tags: Record<string, string>[]) => {
  const tagsSelected = tags.map((tag) => {
    delete tag._text
    delete tag.id
    delete tag.tenantId
    return tag
  })
  store.selectTags(tagsSelected)
}

const saveAzureDiscovery = async () => {
  if (form.validate().length) return
  const success = await store.saveDiscoveryAzure()
  if (success) {
    discoveryQueries.getDiscoveries()
    store.clearAzureForm()
    tagsAutocompleteRef.value.reset()
    props.successCallback(store.azure.name)
  }
}

const nameV = string().required('Name is required.')
const clientIdV = string().required('Client ID is required.')
const clientSecretV = string().required('Client secret is required.')
const subIdV = string().required('Subscription ID is required.')
const dirIdV = string().required('Directory ID is required.')

watchEffect(() => {
  if (props.discovery) {
    form.clearErrors()
    store.azure = { ...store.azure, ...props.discovery }
  }
})
onMounted(() => store.clearAzureForm())
</script>

<style scoped lang="scss">
@use '@/styles/mediaQueriesMixins';
@use '@featherds/styles/themes/variables';
@use '@featherds/styles/mixins/typography';
.azure-container {
  display: flex;
  flex: 1;
  flex-direction: column;

  .title {
    @include typography.headline4;
    margin-bottom: var(variables.$spacing-l);
  }

  hr {
    width: 100%;
    margin-bottom: var(variables.$spacing-xl);
    border-color: var(variables.$shade-4);
  }

  .buttons {
    flex-direction: row;
    align-self: flex-end;
  }

  .locations {
    margin-bottom: var(variables.$spacing-l);
  }

  @include mediaQueriesMixins.screen-md {
    .name,
    .locations,
    .tags {
      width: calc(50% - var(variables.$spacing-s));
    }
    .row {
      display: flex;
      gap: var(variables.$spacing-l);
      .column {
        flex: 1;
      }
    }
  }
}
</style>
