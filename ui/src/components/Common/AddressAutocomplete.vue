<template>
  <div>
    <FeatherAutocomplete
      class="my-autocomplete"
      label="Address"
      type="single"
      v-model="addressModel"
      :loading="loading"
      :results="results"
      @search="(e) => search(e)"
      @update:model-value="(e) => onAddressModelUpdate(e)"
    ></FeatherAutocomplete>
  </div>
</template>

<script lang="ts" setup>
import { OpenStreetMapProvider } from 'leaflet-geosearch'
import { debounce } from 'lodash'
import { IAutocompleteItemType } from '@featherds/autocomplete'
import { PropType } from 'vue'

// List of alternative providers: https://smeijer.github.io/leaflet-geosearch/providers/algolia
const provider = new OpenStreetMapProvider()
const addressModel = ref()
const results = ref([] as IAutocompleteItemType[])
const loading = ref(false)

const search = debounce(async (q: string) => {
  loading.value = true
  const addresses = await provider.search({ query: q })
  results.value = addresses
    .filter((x) => x.label.toLowerCase().indexOf(q) > -1)
    .map((x) => ({
      _text: x.label,
      value: x
    }))
  loading.value = false
}, 1000)

const props = defineProps({
  addressModel: {
    required: true,
    type: Object
  },
  onAddressModelUpdate: {
    type: Function as PropType<(e: any) => void>,
    default: () => ({})
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/mediaQueriesMixins.scss';
</style>
