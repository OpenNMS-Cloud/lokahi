<template>
    <div class="form-title">
      <span>  {{ DiscoverySNMPCopy.title }}</span>
      <FeatherButton secondary @click="deleteDiscovery">Delete Discovery</FeatherButton>
    </div>
    <FeatherInput
      :modelValue="discovery?.name"
      @update:modelValue="(name) => updateSelectedDiscovery('name',name)"
      :label="DiscoverySNMPCopy.nameInputLabel"
      class="name-input"
      data-test="discoveryNameInput"
      :schema="nameV"
      :disabled="isDisabled"
    />
    <div class="locations-select">
      <DiscoveryLocationsAutocomplete :disabled="isDisabled" />
    </div>

    <div class="tags-select">
      <DiscoveryTagsAutocomplete :tags="activeTags" :updateTags="(tags) => activeTags = tags" />
    </div>
   
    <div class="content-editable-container">
      <FeatherTextarea />
      <FeatherTextarea />
      <FeatherTextarea />
    </div>

    <div class="footer">
      <ButtonWithSpinner
        type="submit"
        primary
        data-test="btn-submit"
        :isFetching="isFetchingActiveDiscovery"
      >
        {{ discoveryText.Discovery.button.submit }}
      </ButtonWithSpinner>
    </div>
</template>

<script lang="ts" setup>

import discoveryText, { DiscoverySNMPCopy } from '@/components/Discovery/discovery.text'
import { IcmpActiveDiscovery, IcmpActiveDiscoveryCreateInput, TagCreateInput } from '@/types/graphql'
import { useDiscoveryMutations } from '@/store/Mutations/discoveryMutations'
import { string } from 'yup'
import { useDiscoveryQueries } from '@/store/Queries/discoveryQueries'

const { isFetchingActiveDiscovery } = useDiscoveryMutations()
const discoveryQueries = useDiscoveryQueries()
const activeTags = ref<Array<TagCreateInput>>([])

const props = defineProps<{
  discovery?: IcmpActiveDiscoveryCreateInput,
  updateSelectedDiscovery: (key:string,value:any) => void,
  successCallback: (name: string) => void
  cancel: () => void
}>()

const nameV = string().required('Name is required.')

onMounted(async () => {
  if (props.discovery?.id){
    await discoveryQueries.getTagsByActiveDiscoveryId(props.discovery.id)

    activeTags.value = discoveryQueries.tagsByActiveDiscoveryId
  }
})

const deleteDiscovery = () => {
  console.log('hi')
}

/*
const saveHandler = async () => {
  contentEditableCommunityStringRef.value?.validateContent()
  const isIpInvalid = contentEditableIPRef.value?.validateContent()
  const isPortInvalid = contentEditableUDPPortRef.value?.validateContent()
  if (form.validate().length || isIpInvalid || isPortInvalid) return
  discoveryInfo.value.locationId = discoveryStore.selectedLocation?.id
  
  await createOrUpdateDiscovery({ request: activeDiscoveryFromClientToServerDTO(discoveryInfo.value as IcmpActiveDiscovery) })

  if (!activeDiscoveryError.value && discoveryInfo.value.name) {
    discoveryQueries.getDiscoveries()
    props.successCallback(discoveryInfo.value.name)
    discoveryInfo.value = {} as IcmpActiveDiscoveryCreateInput
  }
} */

const isDisabled = computed(() => Boolean(false))
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
