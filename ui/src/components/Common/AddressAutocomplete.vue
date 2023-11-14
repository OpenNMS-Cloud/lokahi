<template>
  <div>
    <FeatherAutocomplete
      class="my-autocomplete"
      label="Physical Address"
      type="single"
      :modelValue="addressModelValue"
      :minChar="1"
      :loading="loading"
      :results="results"
      @search="(e: any) => search(e)"
      @update:modelValue="(e: any) => $emit('onAddressChange', e)"
    ></FeatherAutocomplete>
  </div>
</template>

<script lang="ts" setup>
import { OpenStreetMapProvider } from 'leaflet-geosearch'
import { debounce } from 'lodash'
import { IAutocompleteItemType } from '@featherds/autocomplete'

const props = defineProps<{
  addressModel: Record<string, any>
}>()

// List of alternative providers: https://smeijer.github.io/leaflet-geosearch/providers/algolia
const provider = new OpenStreetMapProvider()
const results = ref([] as IAutocompleteItemType[])
const loading = ref(false)
const addressModelValue = ref()

watch(
  props,
  async () => {
    await nextTick()
    if (props.addressModel.address) {
      addressModelValue.value = { ...props.addressModel, _text: props.addressModel?.address }
    }
  },
  { immediate: true }
)

const search = debounce(async (q: string) => {
  loading.value = true
  if (q.length == 0) {
    return
  }
  const addresses = await provider.search({ query: q })
  results.value = addresses
    .map((x) => ({
      _text: x.label,
      value: x
    }))
  loading.value = false
}, 1000)
</script>

<style lang="scss" scoped>
@use '@/styles/mediaQueriesMixins.scss';
</style>
