<template>
  <div class="azure-container">
    <div class="title">
      {{ Azure.title }}
    </div>

    <FeatherInput
      label="Azure Name"
      v-model="store.azure.name"
      class="name"
    />

    <div class="row">
      <FeatherInput
        v-model="store.azure.clientId"
        label="ClientID"
        class="column"
      />
      <FeatherProtectedInput
        v-model="store.azure.clientSecret"
        label="Client Secret"
        class="column"
      />
    </div>

    <div class="row">
      <FeatherInput
        v-model="store.azure.subscriptionId"
        label="SubscriptionID"
        class="column"
      />
      <FeatherInput
        v-model="store.azure.directoryId"
        label="DirectoryID"
        class="column"
      />
    </div>

    <LocationsAutocomplete
      @locationSelected="selectLocation"
      class="locations"
    />

    <DiscoveryAutocomplete
      class="tags"
      @items-selected="tagsSelectedListener"
      :get-items="discoveryQueries.getTagsSearch"
      :items="discoveryQueries.tagsSearched"
      :label="Common.tagsInput"
      ref="tagsAutocompleteRef"
    />

    <div class="buttons">
      <FeatherButton
        @click="cancel"
        secondary
      >
        {{ Azure.cancelBtnText }}
      </FeatherButton>
      <ButtonWithSpinner
        :isFetching="discoveryMutations.isFetching.value"
        :disabled="isDisabled"
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
import { Location } from '@/types/graphql'
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { useDiscoveryMutations } from '@/store/Mutations/discoveryMutations'

const store = useDiscoveryStore()
const discoveryQueries = useDiscoveryQueries()
const discoveryMutations = useDiscoveryMutations()

const props = defineProps<{
  successCallback: (name: string) => void
  cancel: () => void
}>()

const selectLocation = (location: Required<Location[]>) =>
  location[0] && location[0].location && store.selectLocation(location[0].location, true)

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

const isDisabled = computed(
  () =>
    !store.azure.name ||
    !store.azure.clientId ||
    !store.azure.clientSecret ||
    !store.azure.directoryId ||
    !store.azure.subscriptionId
)

const saveAzureDiscovery = async () => {
  const success = await store.saveDiscoveryAzure()
  if (success) {
    discoveryQueries.getDiscoveries()
    store.clearAzureForm()
    tagsAutocompleteRef.value.reset()
    props.successCallback(store.azure.name)
  }
}
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
    margin-bottom: var(variables.$spacing-s);
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
