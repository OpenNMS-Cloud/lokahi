<template>
  <form
    @submit.prevent="saveAzureDiscovery"
    novalidate
    class="azure-container"
  >
    <div class="title">
      {{ Azure.title }}
    </div>

    <FeatherInput
      label="Azure Name"
      v-model="store.azure.name"
      :schema="inputV.name"
      required
      class="name"
    />

    <div class="row">
      <FeatherInput
        v-model="store.azure.clientId"
        :schema="inputV.clientID"
        required
        label="ClientID"
        class="column client-id"
      />
      <FeatherProtectedInput
        v-model="store.azure.clientSecret"
        :schema="inputV.clientSecret"
        required
        label="Client Secret"
        class="column client-secret"
      />
    </div>

    <div class="row">
      <FeatherInput
        v-model="store.azure.subscriptionId"
        :schema="inputV.subscriptionID"
        required
        label="SubscriptionID"
        class="column subscription-id"
      />
      <FeatherInput
        v-model="store.azure.directoryId"
        :schema="inputV.directoryID"
        required
        label="DirectoryID"
        class="column directory-id"
      />
    </div>

    <LocationsAutocomplete
      @locationSelected="selectLocation"
      class="locations"
    />

    <DiscoveryAutocomplete
      class="tags"
      @items-selected="tagsSelected"
      :get-items="discoveryQueries.getTagsUponTyping"
      :items="discoveryQueries.tagsUponTyping"
      :label="Common.tagsInput"
    />

    <hr />
    <div class="buttons">
      <FeatherButton
        @click="cancel"
        secondary
      >
        {{ Azure.cancelBtnText }}
      </FeatherButton>
      <ButtonWithSpinner
        :isFetching="discoveryMutations.isFetching.value"
        @click="saveAzureDiscovery"
        primary
        type="submit"
      >
        {{ Azure.saveBtnText }}
      </ButtonWithSpinner>
    </div>
  </form>
</template>

<script setup lang="ts">
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
import { Azure, Common } from './discovery.text'
import useSnackbar from '@/composables/useSnackbar'
import { Location } from '@/types/graphql'
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'
import { useDiscoveryMutations } from '@/store/Mutations/discoveryMutations'
import { string } from 'yup'
import { useForm } from '@featherds/input-helper'

const store = useDiscoveryStore()
const { showSnackbar } = useSnackbar()
const discoveryQueries = useDiscoveryQueries()
const discoveryMutations = useDiscoveryMutations()

const props = defineProps<{
  successCallback: (name: string) => void
  cancel: () => void
}>()

const selectLocation = (location: Required<Location>) => store.selectLocation(location.location, true)

const tagsSelected = (tags: Record<string, string>[]) => console.log('tagsSelected', tags)

const inputV = {
  name: string().required('Required'),
  clientID: string().required('Required'),
  clientSecret: string().required('Required'),
  subscriptionID: string().required('Required'),
  directoryID: string().required('Required')
}
const form = useForm()

const saveAzureDiscovery = async () => {
  const validationErrors = form.validate()
  console.log('validationErrors', validationErrors)
  if (!validationErrors.length) {
    // debugger
    const success = await store.saveDiscoveryAzure()
    if (success) {
      showSnackbar({
        msg: `${store.azure.name} setup successfully.`
      })

      store.clearAzureForm()
      props.successCallback(store.azure.name)
    }
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
  margin-bottom: var(variables.$spacing-xl);

  .title {
    @include typography.headline4;
    margin-bottom: var(variables.$spacing-l);
  }
  .name,
  .client-id,
  .client-secret,
  .subscription-id,
  .directory-id {
    :deep(.feather-input-error) {
      margin-bottom: var(variables.$spacing-l);
    }
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
